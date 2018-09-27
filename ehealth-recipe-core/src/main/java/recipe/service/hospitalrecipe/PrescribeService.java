package recipe.service.hospitalrecipe;

import com.ngari.base.BaseAPI;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientExtendService;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeCommonResTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.hisprescription.model.*;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.wxpay.service.INgariRefundService;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.constant.OrderStatusConstant;
import recipe.constant.PayConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeLogDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.service.RecipeHisService;
import recipe.service.RecipeOrderService;
import recipe.service.hospitalrecipe.dataprocess.PrescribeProcess;

import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/1/31
 * @description： 开方服务
 * @version： 1.0
 */
@RpcBean("remotePrescribeService")
public class PrescribeService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PrescribeService.class);

    /**
     * 新增标识
     */
    public static final int ADD_FLAG = 1;

    /**
     * 撤销标识
     */
    public static final int CANCEL_FLAG = 2;

    /**
     * 更新标识
     */
    public static final int UPDATE_FLAG = 3;

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private RecipeOrderDAO orderDAO;

    @Autowired
    private RecipeLogDAO recipeLogDAO;

    /**
     * 创建处方
     *
     * @param recipeInfo 处方json格式数据
     * @return
     */
    @RpcService
    public HosRecipeResult createPrescription(HospitalRecipeDTO hospitalRecipeDTO) {
        HosRecipeResult<RecipeBean> result = new HosRecipeResult();
        if (null != hospitalRecipeDTO) {
            //TODO 修改校验模块通过@Verify注解来处理
            result = PrescribeProcess.validateHospitalRecipe(hospitalRecipeDTO, ADD_FLAG);
            if (HosRecipeResult.FAIL.equals(result.getCode())) {
                return result;
            }
            //重置为默认失败
            result.setCode(HosRecipeResult.FAIL);

            RecipeBean recipe = new RecipeBean();

            //转换组织结构编码
            Integer clinicOrgan = null;
            try {
                OrganBean organ = getOrganByOrganId(hospitalRecipeDTO.getOrganId());
                if (null != organ) {
                    clinicOrgan = organ.getOrganId();
                    //后面处理会用到
                    hospitalRecipeDTO.setClinicOrgan(clinicOrgan.toString());
                    recipe.setClinicOrgan(clinicOrgan);
                    recipe.setOrganName(organ.getShortName());
                }
            } catch (Exception e) {
                LOG.warn("createPrescription 查询机构异常，organId={}", hospitalRecipeDTO.getOrganId(), e);
            } finally {
                if (null == clinicOrgan) {
                    LOG.warn("createPrescription 平台未匹配到该组织机构编码，organId={}", hospitalRecipeDTO.getOrganId());
                    result.setMsg("平台未匹配到该组织机构编码");
                    return result;
                }
            }

            String recipeCode = hospitalRecipeDTO.getRecipeCode();
            Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(recipeCode, clinicOrgan);
            //TODO 通过某种条件判断处方内容是否相同再执行后续
            //当前处理为存在处方则返回，不做更新处理
            if (null != dbRecipe) {
                result.setCode(HosRecipeResult.DUPLICATION);
                result.setMsg("处方已存在");
                return result;
            }

            //设置医生信息
            EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
            EmploymentDTO employment = null;
            try {
                employment = employmentService.getByJobNumberAndOrganId(
                        hospitalRecipeDTO.getDoctorNumber(), clinicOrgan);

            } catch (Exception e) {
                LOG.warn("createPrescription 查询医生执业点异常，doctorNumber={}, clinicOrgan={}",
                        hospitalRecipeDTO.getDoctorNumber(), clinicOrgan, e);
            } finally {
                if (null != employment) {
                    recipe.setDoctor(employment.getDoctorId());
                    recipe.setDepart(employment.getDepartment());

                    //审核医生信息处理
                    String checkerNumber = hospitalRecipeDTO.getCheckerNumber();
                    if (StringUtils.isNotEmpty(checkerNumber)) {
                        EmploymentDTO checkEmployment = employmentService.getByJobNumberAndOrganId(
                                checkerNumber, Integer.valueOf(hospitalRecipeDTO.getCheckOrgan()));
                        if (null != checkEmployment) {
                            recipe.setChecker(checkEmployment.getDoctorId());
                        } else {
                            LOG.warn("createPrescription 审核医生[{}]在平台没有执业点", checkerNumber);
                        }
                    } else {
                        LOG.info("createPrescription 审核医生工号(checkerNumber)为空");
                    }
                } else {
                    LOG.warn("createPrescription 平台未找到该医生执业点，doctorNumber={}, clinicOrgan={}",
                            hospitalRecipeDTO.getDoctorNumber(), clinicOrgan);
                    result.setMsg("平台未找到该医生执业点");
                    return result;
                }
            }

            //处理患者
            //先查询就诊人是否存在
            PatientBean patient = null;
            try {
                IPatientExtendService patientExtendService = BaseAPI.getService(IPatientExtendService.class);
                List<PatientBean> patList = patientExtendService.findPatient4Doctor(recipe.getDoctor(),
                        hospitalRecipeDTO.getCertificate());
                if (CollectionUtils.isEmpty(patList)) {
                    patient = new PatientBean();
                    patient.setPatientName(hospitalRecipeDTO.getPatientName());
                    patient.setPatientSex(hospitalRecipeDTO.getPatientSex());
                    patient.setCertificateType(Integer.valueOf(hospitalRecipeDTO.getCertificateType()));
                    patient.setCertificate(hospitalRecipeDTO.getCertificate());
                    patient.setAddress(hospitalRecipeDTO.getPatientAddress());
                    patient.setMobile(hospitalRecipeDTO.getPatientTel());
                    //创建就诊人
                    patient = patientExtendService.addPatient4DoctorApp(patient, 0);
                } else {
                    patient = patList.get(0);
                }
            } catch (Exception e) {
                LOG.warn("createPrescription 处理就诊人异常，doctorNumber={}, clinicOrgan={}",
                        hospitalRecipeDTO.getDoctorNumber(), clinicOrgan, e);
            } finally {
                if (null == patient || StringUtils.isEmpty(patient.getMpiId())) {
                    LOG.warn("createPrescription 患者创建失败，doctorNumber={}, clinicOrgan={}",
                            hospitalRecipeDTO.getDoctorNumber(), clinicOrgan);
                    result.setMsg("患者创建失败");
                    return result;
                } else {
                    recipe.setPatientName(patient.getPatientName());
                    recipe.setPatientStatus(1); //有效
                    recipe.setMpiid(patient.getMpiId());
                }
            }

            //设置其他参数
            PrescribeProcess.convertNgariRecipe(recipe, hospitalRecipeDTO);

            //设置为医院HIS获取的处方，不会在医生端列表展示数据
            //0:表示HIS处方，不会在任何地方展示
            //1:平台开具处方，平台处理业务都会展示
            //2:HIS处方，只在药师审核处展示
            recipe.setFromflag(RecipeBussConstant.FROMFLAG_HIS_USE);

            //创建详情数据
            List<RecipeDetailBean> details = null;
            details = PrescribeProcess.convertNgariDetail(hospitalRecipeDTO);
            if (CollectionUtils.isEmpty(details)) {
                LOG.warn("createPrescription 药品详情转换错误, hospitalRecipeDTO={}", JSONUtils.toString(hospitalRecipeDTO));
                result.setMsg("药品详情转换错误");
                return result;
            }

            //写入DB
            try {
                Integer recipeId = recipeDAO.updateOrSaveRecipeAndDetail(
                        ObjectCopyUtils.convert(recipe, Recipe.class),
                        ObjectCopyUtils.convert(details, Recipedetail.class), false);
                LOG.info("createPrescription 写入DB成功. recipeId={}", recipeId);
                recipeLogDAO.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "医院处方接收成功");
                recipe.setRecipeId(recipeId);
                result.setData(recipe);
                result.setCode(HosRecipeResult.SUCCESS);
            } catch (Exception e) {
                LOG.error("createPrescription 写入DB失败. recipe={}, detail={}", JSONUtils.toString(recipe),
                        JSONUtils.toString(details), e);
                result.setMsg("写入DB失败");
            }

        } else {
            LOG.warn("createPrescription recipe is empty.");
            result.setMsg("处方对象为空");
        }

        return result;
    }

    /**
     * 处方状态更新
     * @param request
     * @return
     */
    public HosRecipeResult updateRecipeStatus(HospitalStatusUpdateDTO request) {
        HosRecipeResult result = new HosRecipeResult();
        if (null != request) {
            //TODO 修改校验模块通过@Verify注解来处理
            if(StringUtils.isEmpty(request.getOrganId()) || StringUtils.isEmpty(request.getRecipeCode())
                    || StringUtils.isEmpty(request.getStatus())){
                result.setCode(HosRecipeResult.FAIL);
                result.setMsg("request对象必填参数为空");
                return result;
            }

            //重置默认为失败
            result.setCode(HosRecipeResult.FAIL);

            //转换组织结构编码
            Integer clinicOrgan = null;
            try {
                OrganBean organ = getOrganByOrganId(request.getOrganId());
                if(null != organ){
                    clinicOrgan = organ.getOrganId();
                }
            } catch (Exception e) {
                LOG.warn("updateRecipeStatus 查询机构异常，organId={}", request.getOrganId(), e);
            } finally {
                if (null == clinicOrgan) {
                    LOG.warn("updateRecipeStatus 平台未匹配到该组织机构编码，organId={}", request.getOrganId());
                    result.setMsg("平台未匹配到该组织机构编码");
                    return result;
                }
            }

            Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(request.getRecipeCode(), clinicOrgan);
            //数据对比
            if (null == dbRecipe) {
                result.setMsg("不存在该处方");
                return result;
            }
            Integer status = Integer.valueOf(request.getStatus());
            if (status.equals(dbRecipe.getStatus())) {
                result.setCode(HosRecipeResult.SUCCESS);
                result.setMsg("处方状态相同");
                return result;
            }

            //支持状态改变的情况判断
            if(!(RecipeStatusConstant.DELETE == status)){
                result.setMsg("不支持的处方状态改变");
                return result;
            }

            //作废处理
            if(RecipeStatusConstant.DELETE == status) {
                if (RecipeStatusConstant.WAIT_SEND == dbRecipe.getStatus()
                        || RecipeStatusConstant.IN_SEND == dbRecipe.getStatus()
                        || RecipeStatusConstant.FINISH == dbRecipe.getStatus()) {
                    result.setMsg("该处方已处于配送状态，无法撤销");
                    return result;
                }

                //取消订单数据
                RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                //如果已付款则需要进行退款
                RecipeOrder order = orderDAO.getByOrderCode(dbRecipe.getOrderCode());
                orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO);
                //取消处方单
                recipeDAO.updateRecipeInfoByRecipeId(dbRecipe.getRecipeId(), status, null);

                if (PayConstant.PAY_FLAG_PAY_SUCCESS == order.getPayFlag()) {
                    //已支付的情况需要退款
                    try {
                        INgariRefundService rufundService = BaseAPI.getService(INgariRefundService.class);
                        rufundService.refund(order.getOrderId(), "recipe");
                    } catch (Exception e) {
                        LOG.warn("updateRecipeStatus 退款异常，orderId={}", order.getOrderId(), e);
                        recipeLogDAO.saveRecipeLog(dbRecipe.getRecipeId(), RecipeStatusConstant.UNKNOW,
                                RecipeStatusConstant.UNKNOW, "医院处方-退款异常");
                    } finally {
                        //HIS消息-退款
                        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
                        hisService.recipeRefund(dbRecipe.getRecipeId());
                    }
                }
                recipeLogDAO.saveRecipeLog(dbRecipe.getRecipeId(), dbRecipe.getStatus(), status, "医院处方作废成功");
                result.setCode(HosRecipeResult.SUCCESS);
            }
        } else {
            result.setCode(HosRecipeResult.FAIL);
            result.setMsg("request对象为空");
        }

        return result;
    }

    /**
     * 查询处方
     *
     * @param searchQO 查询条件
     * @return
     */
    public HosBussResult getPrescription(HospitalSearchQO searchQO) {
        HosBussResult response = new HosBussResult();
        if (null == searchQO || StringUtils.isEmpty(searchQO.getClinicOrgan())
                || StringUtils.isEmpty(searchQO.getRecipeCode())) {
            response.setCode(RecipeCommonResTO.FAIL);
            response.setMsg("查询参数缺失");
            return response;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(searchQO.getRecipeCode(),
                Integer.parseInt(searchQO.getClinicOrgan()));
        if (null != dbRecipe) {
            HospitalRecipeDTO hospitalRecipeDTO = PrescribeProcess.convertHospitalRecipe(dbRecipe);
            if (null != hospitalRecipeDTO) {
                response.setCode(RecipeCommonResTO.SUCCESS);
                response.setPrescription(hospitalRecipeDTO);
                //TODO 物流信息设置
                return response;
            }

            response.setCode(RecipeCommonResTO.FAIL);
            response.setMsg("编号为[" + searchQO.getRecipeCode() + "]处方获取失败");
            return response;
        }

        response.setCode(RecipeCommonResTO.FAIL);
        response.setMsg("找不到编号为[" + searchQO.getRecipeCode() + "]处方");
        return response;
    }

    /**
     * 根据[组织机构编码]获取平台医院机构
     *
     * @param organId
     * @return
     * @throws Exception
     */
    public OrganBean getOrganByOrganId(String organId) throws Exception {
        IOrganService organService = BaseAPI.getService(IOrganService.class);
        OrganBean organ = null;
        List<OrganBean> organList = organService.findByOrganizeCode(organId);
        if (CollectionUtils.isNotEmpty(organList)) {
            organ = organList.get(0);
        }

        return organ;
    }


}
