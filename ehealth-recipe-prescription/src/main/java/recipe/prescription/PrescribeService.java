package recipe.prescription;

import com.google.common.collect.Maps;
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
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.prescription.bean.HosRecipeResult;
import recipe.prescription.bean.HospitalRecipeDTO;
import recipe.prescription.dataprocess.PrescribeProcess;
import recipe.util.ApplicationUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe dbRecipe = recipeDAO.getByOriginRecipeCodeAndOriginClinicOrgan(hospitalRecipeDTO.getRecipeCode(),
                    Integer.parseInt(hospitalRecipeDTO.getClinicOrgan()));
            if (null != dbRecipe) {
                result.setCode(CommonConstant.FAIL);
                result.setMsg("平台已存在该处方");
                return result;
            }

            Recipe recipe = PrescribeProcess.convertNgariRecipe(hospitalRecipeDTO);
            if (null != recipe) {
                IOrganService organService = ApplicationUtils.getBaseService(IOrganService.class);
                Integer originClinicOrgan = recipe.getOriginClinicOrgan();
                OrganBean organ = organService.get(originClinicOrgan);
                if (null == organ) {
                    result.setCode(CommonConstant.FAIL);
                    result.setMsg("平台未找到相关机构");
                    return result;
                }

                IEmploymentService employmentService = ApplicationUtils.getBaseService(IEmploymentService.class);
                //设置医生信息
                EmploymentBean employment = employmentService.getByJobNumberAndOrganId(
                        hospitalRecipeDTO.getDoctorNumber(), originClinicOrgan);
                if (null != employment) {
                    recipe.setDoctor(employment.getDoctorId());
                    recipe.setDepart(employment.getDepartment());

                    //审核医生信息处理
                    String checkerNumber = hospitalRecipeDTO.getCheckerNumber();
                    if (StringUtils.isNotEmpty(checkerNumber)) {
                        EmploymentBean checkEmployment = employmentService.getByJobNumberAndOrganId(
                                checkerNumber, originClinicOrgan);
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
                        result.setCode(CommonConstant.FAIL);
                        result.setMsg("获取平台患者失败");
                        return result;
                    } else {
                        //创建患者
                        recipe.setMpiid(patient.getMpiId());
                        recipe.setPatientName(hospitalRecipeDTO.getPatientName());
                    }

                    //创建详情数据
                    List<Recipedetail> details = PrescribeProcess.convertNgariDetail(hospitalRecipeDTO);
                    if (CollectionUtils.isEmpty(details)) {
                        result.setCode(CommonConstant.FAIL);
                        result.setMsg("存在下级医院无法开具的药品");
                        return result;
                    }

                    //初始化处方状态为待处理
                    recipe.setStatus(RecipeStatusConstant.CHECK_PASS);
                    Integer recipeId = null;
                    if (null != recipeId) {

                    } else {
                        result.setCode(CommonConstant.FAIL);
                        result.setMsg("处方创建失败");
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
