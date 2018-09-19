package recipe.prescription;

import com.ngari.base.BaseAPI;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientExtendService;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeCommonResTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.hisprescription.model.HosBussResult;
import com.ngari.recipe.hisprescription.model.HosRecipeResult;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.hisprescription.model.HospitalSearchQO;
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
import recipe.common.CommonConstant;
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
        HosRecipeResult result = new HosRecipeResult();
        if (null != hospitalRecipeDTO) {
            if (StringUtils.isNotEmpty(hospitalRecipeDTO.getClinicOrgan())
                    && StringUtils.isNotEmpty(hospitalRecipeDTO.getRecipeCode())) {

                Integer clinicOrgan = Integer.valueOf(hospitalRecipeDTO.getClinicOrgan());
                String recipeCode = hospitalRecipeDTO.getRecipeCode();
                Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, clinicOrgan);
                //TODO 通过某种条件判断处方内容是否相同再执行后续
                //当前处理为存在处方则返回，不做更新处理
                if (null != dbRecipe) {
                    result.setCode(HosRecipeResult.SUCCESS);
                    result.setMsg("处方已存在");
                    return result;
                }

                //TODO 修改校验模块通过@Verify注解来处理
                result = PrescribeProcess.validateHospitalRecipe(hospitalRecipeDTO, ADD_FLAG);
                if (CommonConstant.FAIL.equals(result.getCode())) {
                    return result;
                }

                RecipeBean recipe = new RecipeBean();

                //校验机构等数据合法性
                OrganService organService = BasicAPI.getService(OrganService.class);
                String organName = organService.getShortNameById(clinicOrgan);
                if (StringUtils.isEmpty(organName)) {
                    result.setCode(HosRecipeResult.FAIL);
                    result.setMsg("平台未找到相关机构");
                    return result;
                } else {
                    recipe.setClinicOrgan(clinicOrgan);
                    recipe.setOrganName(organName);
                }

                //设置医生信息
                EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
                EmploymentDTO employment = employmentService.getByJobNumberAndOrganId(
                        hospitalRecipeDTO.getDoctorNumber(), clinicOrgan);
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
                    LOG.warn("createPrescription 医生未找到平台执业点。doctorNumber={}, organId={}",
                            hospitalRecipeDTO.getDoctorNumber(), clinicOrgan);
                    result.setCode(HosRecipeResult.FAIL);
                    result.setMsg("患者创建失败");
                    return result;
                }

                //处理患者
                //先查询就诊人是否存在
                IPatientExtendService patientExtendService = BaseAPI.getService(IPatientExtendService.class);
                List<PatientBean> patList = patientExtendService.findCurrentUserPatientList(hospitalRecipeDTO.getCertificate());
                PatientBean patient;
                if (CollectionUtils.isEmpty(patList)) {
                    patient = new PatientBean();
                    patient.setPatientName(hospitalRecipeDTO.getPatientName());
                    patient.setPatientSex(hospitalRecipeDTO.getPatientSex());
                    patient.setCertificateType(Integer.valueOf(hospitalRecipeDTO.getCertificateType()));
                    patient.setCertificate(hospitalRecipeDTO.getCertificate());
                    patient.setAddress(hospitalRecipeDTO.getPatientAddress());
                    patient.setMobile(hospitalRecipeDTO.getPatientTel());
                    //创建就诊人
                    try {
                        patient = patientExtendService.addPatient4DoctorApp(patient, 0);
                    } catch (Exception e) {
                        LOG.warn("createPrescription 患者创建失败。patient={}", JSONUtils.toString(patient), e);
                    }
                } else {
                    patient = patList.get(0);
                }

                if (StringUtils.isEmpty(patient.getMpiId())) {
                    LOG.warn("createPrescription 患者存在异常，patient={}", JSONUtils.toString(patient));
                    result.setCode(HosRecipeResult.FAIL);
                    result.setMsg("患者创建失败");
                    return result;
                } else {
                    recipe.setPatientName(patient.getPatientName());
                    recipe.setPatientStatus(1); //有效
                    recipe.setMpiid(patient.getMpiId());
                }

                //设置其他参数
                PrescribeProcess.convertNgariRecipe(recipe, hospitalRecipeDTO);

                //创建详情数据
                List<RecipeDetailBean> details = PrescribeProcess.convertNgariDetail(hospitalRecipeDTO);
                if (CollectionUtils.isEmpty(details)) {
                    LOG.warn("createPrescription 药品详情转换错误, hospitalRecipeDTO={}", JSONUtils.toString(hospitalRecipeDTO));
                    result.setCode(HosRecipeResult.FAIL);
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
                } catch (Exception e) {
                    LOG.error("createPrescription 写入DB失败. recipe={}, detail={}", JSONUtils.toString(recipe),
                            JSONUtils.toString(details), e);
                    result.setCode(HosRecipeResult.FAIL);
                    result.setMsg("写入DB失败");
                }
            }

            return result;
        } else {
            LOG.error("createPrescription recipe is empty.");
            result.setCode(HosRecipeResult.FAIL);
            result.setMsg("处方对象为空");
            return result;
        }
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
