package recipe.prescription;

import com.ngari.base.employment.model.EmploymentBean;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.common.CommonConstant;
import recipe.common.ResponseUtils;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeLogDAO;
import recipe.prescription.bean.HosBussResult;
import recipe.prescription.bean.HosRecipeResult;
import recipe.prescription.bean.HospitalRecipeDTO;
import recipe.prescription.bean.HospitalSearchQO;
import recipe.prescription.dataprocess.PrescribeProcess;
import recipe.util.ApplicationUtils;

import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/1/31
 * @description： 开方服务
 * @version： 1.0
 */
@RpcBean("prescribeService")
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
    private static final int CANCEL_FLAG = 2;

    /**
     * 更新标识
     */
    private static final int UPDATE_FLAG = 3;

    /**
     * 创建处方
     *
     * @param recipeInfo 处方json格式数据
     * @return
     */
    @RpcService
    public HosRecipeResult createPrescription(HospitalRecipeDTO hospitalRecipeDTO) {
        if (null != hospitalRecipeDTO) {
            HosRecipeResult result = PrescribeProcess.validateHospitalRecipe(hospitalRecipeDTO, ADD_FLAG);
            result.setRecipeCode(hospitalRecipeDTO.getRecipeCode());
            if (CommonConstant.FAIL.equals(result.getCode())) {
                return result;
            }

            OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
            String organName = organService.getShortNameById(Integer.parseInt(hospitalRecipeDTO.getClinicOrgan()));
            if (StringUtils.isEmpty(organName)) {
                result.setCode(CommonConstant.FAIL);
                result.setMsg("平台未找到相关机构");
                return result;
            }

            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);

            Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(hospitalRecipeDTO.getRecipeCode(),
                    Integer.parseInt(hospitalRecipeDTO.getClinicOrgan()));
            if (null != dbRecipe) {
                result.setRecipeId(dbRecipe.getRecipeId());
                result.setCode(CommonConstant.FAIL);
                result.setMsg("平台已存在该处方");
                return result;
            }

            Recipe recipe = PrescribeProcess.convertNgariRecipe(hospitalRecipeDTO);
            if (null != recipe) {
                recipe.setOrganName(organName);
                IEmploymentService employmentService = ApplicationUtils.getBaseService(IEmploymentService.class);
                //设置医生信息
                EmploymentBean employment = employmentService.getByJobNumberAndOrganId(
                        hospitalRecipeDTO.getDoctorNumber(), recipe.getClinicOrgan());
                if (null != employment) {
                    recipe.setDoctor(employment.getDoctorId());
                    recipe.setDepart(employment.getDepartment());

                    //审核医生信息处理
                    String checkerNumber = hospitalRecipeDTO.getCheckerNumber();
                    if (StringUtils.isNotEmpty(checkerNumber)) {
                        EmploymentBean checkEmployment = employmentService.getByJobNumberAndOrganId(
                                checkerNumber, recipe.getCheckOrgan());
                        if (null != checkEmployment) {
                            recipe.setChecker(checkEmployment.getDoctorId());
                        } else {
                            LOG.warn("createPrescription 审核医生在平台没有执业点");
                        }
                    } else {
                        LOG.warn("createPrescription 审核医生工号(checkerNumber)为空");
                    }

                    PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
                    //TODO 获取的患者是什么类型的
                    PatientDTO patient = patientService.getByIdCard(hospitalRecipeDTO.getCertificate());
                    if (null == patient) {
                        //TODO 创建患者 ...
                        result.setCode(CommonConstant.FAIL);
                        result.setMsg("获取平台患者失败");
                        return result;
                    } else {
                        recipe.setMpiid(patient.getMpiId());
                        recipe.setPatientStatus(patient.getStatus());
                    }

                    //创建详情数据
                    List<Recipedetail> details = PrescribeProcess.convertNgariDetail(hospitalRecipeDTO);
                    if (CollectionUtils.isEmpty(details)) {
                        result.setCode(CommonConstant.FAIL);
                        result.setMsg("药品详情转换错误");
                        return result;
                    }

                    //写入DB
                    try {
                        Integer recipeId = recipeDAO.updateOrSaveRecipeAndDetail(recipe, details, false);
                        LOG.info("createPrescription 写入DB成功. recipeId={}", recipeId);
                        result.setRecipeId(recipeId);
                        result.setRecipe(recipe);
                        result.setHospitalRecipe(hospitalRecipeDTO);
                        recipeLogDAO.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS,
                                RecipeStatusConstant.CHECK_PASS, "医院处方接收成功");
                    } catch (Exception e) {
                        LOG.error("createPrescription 写入DB失败. recipe={}, detail={}", JSONUtils.toString(recipe),
                                JSONUtils.toString(details), e);
                        result.setCode(CommonConstant.FAIL);
                        result.setMsg("写入DB失败");
                    }
                } else {
                    result.setCode(CommonConstant.FAIL);
                    result.setMsg("该医生没有执业点");
                }
            } else {
                result.setCode(CommonConstant.FAIL);
                result.setMsg("处方转换失败");
            }

            return result;
        } else {
            LOG.error("createPrescription recipe is empty.");
            return ResponseUtils.getFailResponse(HosRecipeResult.class, "未知错误-处方对象为空");
        }
    }

    /**
     * 查询处方
     *
     * @param searchQO 查询条件
     * @return
     */
    public HosBussResult getPrescription(HospitalSearchQO searchQO) {
        if (null == searchQO || StringUtils.isEmpty(searchQO.getClinicOrgan())
                || StringUtils.isEmpty(searchQO.getRecipeCode())) {
            return ResponseUtils.getFailResponse(HosBussResult.class, "查询参数缺失");
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(searchQO.getRecipeCode(),
                Integer.parseInt(searchQO.getClinicOrgan()));
        if (null != dbRecipe) {
            HospitalRecipeDTO hospitalRecipeDTO = PrescribeProcess.convertHospitalRecipe(dbRecipe);
            if (null != hospitalRecipeDTO) {
                HosBussResult result = ResponseUtils.getSuccessResponse(HosBussResult.class);
                result.setPrescription(hospitalRecipeDTO);
                //TODO 物流信息设置
                return result;
            }

            return ResponseUtils.getFailResponse(HosBussResult.class, "编号为[" + searchQO.getRecipeCode() + "]处方获取失败");
        }

        return ResponseUtils.getFailResponse(HosBussResult.class, "找不到编号为[" + searchQO.getRecipeCode() + "]处方");
    }

    /**
     * 撤销处方
     *
     * @param searchQO 查询条件
     * @return
     */
    @RpcService
    public HosBussResult cancelPrescription(HospitalSearchQO searchQO) {
        if (null == searchQO || StringUtils.isEmpty(searchQO.getClinicOrgan())
                || StringUtils.isEmpty(searchQO.getRecipeCode())) {
            return ResponseUtils.getFailResponse(HosBussResult.class, "查询参数缺失");
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);

        Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(searchQO.getRecipeCode(),
                Integer.parseInt(searchQO.getClinicOrgan()));
        if (null != dbRecipe) {
            Boolean result = recipeDAO.updateRecipeInfoByRecipeId(dbRecipe.getRecipeId(),
                    RecipeStatusConstant.REVOKE, null);
            if (!result) {
                return ResponseUtils.getFailResponse(HosBussResult.class, "编号为[" + searchQO.getRecipeCode() + "]处方更新失败");
            }

            //记录日志
            recipeLogDAO.saveRecipeLog(dbRecipe.getRecipeId(), dbRecipe.getStatus(),
                    RecipeStatusConstant.REVOKE, "医院处方撤销成功");
            return ResponseUtils.getSuccessResponse(HosRecipeResult.class);
        }

        return ResponseUtils.getFailResponse(HosBussResult.class, "找不到编号为[" + searchQO.getRecipeCode() + "]处方");
    }


}
