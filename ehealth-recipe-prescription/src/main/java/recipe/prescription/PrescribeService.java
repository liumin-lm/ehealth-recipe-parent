package recipe.prescription;

import com.ngari.base.employment.model.EmploymentBean;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
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
import recipe.dao.RecipeDAO;
import recipe.prescription.bean.HosRecipeResult;
import recipe.prescription.bean.HospitalRecipeDTO;
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
     * @param recipeInfo
     * @return
     */
    @RpcService
    public HosRecipeResult createPrescription(String recipeInfo) {
        if (StringUtils.isEmpty(recipeInfo)) {
            return ResponseUtils.getFailResponse(HosRecipeResult.class, "传入参数为空");
        }

        HospitalRecipeDTO hospitalRecipeDTO = null;
        try {
            hospitalRecipeDTO = JSONUtils.parse(recipeInfo, HospitalRecipeDTO.class);
        } catch (Exception e) {
            LOG.error("createPrescription parse error. param={}", recipeInfo, e);
            return ResponseUtils.getFailResponse(HosRecipeResult.class, "解析出错");
        }

        if (null != hospitalRecipeDTO) {
            HosRecipeResult result = PrescribeProcess.validateHospitalRecipe(hospitalRecipeDTO, ADD_FLAG);
            result.setRecipeCode(hospitalRecipeDTO.getRecipeCode());
            if (CommonConstant.FAIL.equals(result.getCode())) {
                return result;
            }

            IOrganService organService = ApplicationUtils.getBaseService(IOrganService.class);
            OrganBean organ = organService.get(Integer.parseInt(hospitalRecipeDTO.getClinicOrgan()));
            if (null == organ) {
                result.setCode(CommonConstant.FAIL);
                result.setMsg("平台未找到相关机构");
                return result;
            }

            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

            Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrgan(hospitalRecipeDTO.getRecipeCode(),
                    Integer.parseInt(hospitalRecipeDTO.getClinicOrgan()));
            if (null != dbRecipe) {
                result.setRecipeId(dbRecipe.getRecipeId());
                result.setCode(CommonConstant.FAIL);
                result.setMsg("平台已存在该处方");
                return result;
            }

            Recipe recipe = PrescribeProcess.convertNgariRecipe(hospitalRecipeDTO);
            if (null != recipe) {
                recipe.setOrganName(organ.getShortName());
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
                            LOG.error("createPrescription 审核医生在平台没有执业点");
                        }
                    } else {
                        LOG.error("createPrescription 审核医生工号(checkerNumber)为空");
                    }

                    IPatientService patientService = ApplicationUtils.getBaseService(IPatientService.class);
                    PatientBean patient = patientService.getByIdCard(hospitalRecipeDTO.getCertificate());
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
            LOG.error("createPrescription recipeList is empty. param={}", recipeInfo);
            return ResponseUtils.getFailResponse(HosRecipeResult.class, "未知错误-处方对象为空");
        }
    }


}
