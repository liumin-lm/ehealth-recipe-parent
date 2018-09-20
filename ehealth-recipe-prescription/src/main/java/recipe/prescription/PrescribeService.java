package recipe.prescription;

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
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.hisprescription.model.*;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeLogDAO;
import recipe.prescription.dataprocess.PrescribeProcess;

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
                String organIdStr = hospitalRecipeDTO.getOrganId();
                IOrganService organService = BaseAPI.getService(IOrganService.class);
                List<OrganBean> organList = organService.findByOrganizeCode(organIdStr);
                if (CollectionUtils.isNotEmpty(organList)) {
                    OrganBean organ = organList.get(0);
                    clinicOrgan = organ.getOrganId();
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
            Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, clinicOrgan);
            //TODO 通过某种条件判断处方内容是否相同再执行后续
            //当前处理为存在处方则返回，不做更新处理
            if (null != dbRecipe) {
                result.setCode(HosRecipeResult.SUCCESS);
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
                List<PatientBean> patList = patientExtendService.findCurrentUserPatientList(hospitalRecipeDTO.getCertificate());
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
            recipe.setFromflag(2);

            //创建详情数据
            List<RecipeDetailBean> details = PrescribeProcess.convertNgariDetail(hospitalRecipeDTO);
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

    public HosRecipeResult updateRecipeStatus(HospitalStatusUpdateDTO request) {
        HosRecipeResult result = new HosRecipeResult();
        if (null != request) {
            //TODO 修改校验模块通过@Verify注解来处理
//            result = PrescribeProcess.validateHospitalRecipe(hospitalRecipeDTO, ADD_FLAG);
//            if (HosRecipeResult.FAIL.equals(result.getCode())) {
//                return result;
//            }
            Integer clinicOrgan = Integer.valueOf(request.getClinicOrgan());
            String recipeCode = request.getRecipeCode();
            Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, clinicOrgan);
            //TODO 数据对比
            if (null == dbRecipe) {
//                LOG.warn("updateRecipeStatus 不存在该处方. request={}", JSONUtils.toString(request));
                result.setCode(HosRecipeResult.FAIL);
                result.setMsg("不存在该处方");
                return result;
            }
            Integer status = Integer.valueOf(request.getStatus());
            if (status.equals(dbRecipe.getStatus())) {
//                LOG.info("updateRecipeStatus 处方状态相同. request={}", JSONUtils.toString(request));
                result.setCode(HosRecipeResult.SUCCESS);
                result.setMsg("处方状态相同");
                return result;
            }

            //TODO 如果已付款则需要进行退款
//            try {
//                //退款
//                PaymentBean paymentBean = new PaymentBean();
//                paymentBean.setPaymentType("WX");
//                paymentBean.setBusType(RecipeService.WX_RECIPE_BUSTYPE);
//                paymentBean.setOrderId(order.getOrderId());
//                IPaymentService paymentService = BaseAPI.getService(IPaymentService.class);
//                paymentService.refund(paymentBean);
//            } catch (Exception e) {
//                LOGGER.error("wxPayRefundForRecipe " + errorInfo + "*****微信退款异常！recipeId[" + recipeId + "],err[" + e.getMessage() + "]");
//            }
//            //取消订单数据
//            orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO);
//            //取消处方单
//            recipeDAO.updateRecipeInfoByRecipeId

            recipeLogDAO.saveRecipeLog(dbRecipe.getRecipeId(), dbRecipe.getStatus(), RecipeStatusConstant.DELETE, "医院处方作废成功");
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
     * 撤销处方
     *
     * @param searchQO 查询条件
     * @return
     */
    @RpcService
    public HosBussResult cancelPrescription(HospitalSearchQO searchQO) {
        HosBussResult response = new HosBussResult();
        if (null == searchQO || StringUtils.isEmpty(searchQO.getClinicOrgan())
                || StringUtils.isEmpty(searchQO.getRecipeCode())) {
            response.setCode(RecipeCommonResTO.FAIL);
            response.setMsg("查询参数缺失");
            return response;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);

        Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(searchQO.getRecipeCode(),
                Integer.parseInt(searchQO.getClinicOrgan()));
        if (null != dbRecipe) {
            Boolean result = recipeDAO.updateRecipeInfoByRecipeId(dbRecipe.getRecipeId(),
                    RecipeStatusConstant.REVOKE, null);
            if (!result) {
                response.setCode(RecipeCommonResTO.FAIL);
                response.setMsg("编号为[" + searchQO.getRecipeCode() + "]处方更新失败");
                return response;
            }

            //记录日志
            recipeLogDAO.saveRecipeLog(dbRecipe.getRecipeId(), dbRecipe.getStatus(),
                    RecipeStatusConstant.REVOKE, "医院处方撤销成功");
            response.setCode(RecipeCommonResTO.SUCCESS);
            return response;
        }

        response.setCode(RecipeCommonResTO.FAIL);
        response.setMsg("找不到编号为[" + searchQO.getRecipeCode() + "]处方");
        return response;
    }


}
