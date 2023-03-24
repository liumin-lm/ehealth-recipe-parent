package recipe.core.api.patient;

import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.vo.*;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.patient.PatientRecipeDetailReqVO;
import recipe.vo.patient.PatientRecipeDetailResVO;
import recipe.vo.patient.ReadyRecipeVO;

import java.util.List;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2021\7\30 0030 09:52
 */
public interface IPatientBusinessService {

    /**
     * 校验当前就诊人是否有效 是否实名认证 就诊卡是否有效
     *
     * @param outPatientReqVO 当前就诊人信息
     * @return 枚举值
     */
    Integer checkCurrentPatient(OutPatientReqVO outPatientReqVO);

    /**
     * 医保授权操作
     * @param medicalInsuranceAuthInfoVO
     * @return
     */
    MedicalInsuranceAuthResVO medicalInsuranceAuth(MedicalInsuranceAuthInfoVO medicalInsuranceAuthInfoVO);

    /**
     * 根据mpiId获取患者信息
     *
     * @param mpiId 患者唯一号
     * @return 患者信息
     */
    PatientDTO getPatientDTOByMpiID(String mpiId);

    /**
     * 根据mpiId获取患者信息
     *
     * @param mpiIds 患者唯一号
     * @return 患者信息
     */
    Map<String, com.ngari.recipe.dto.PatientDTO> findPatientByMpiIds(List<String> mpiIds);

    /**
     * 获取患者医保信息
     *
     * @param patientInfoVO 患者信息
     * @return 医保类型相关
     */
    PatientMedicalTypeVO queryPatientMedicalType(PatientInfoVO patientInfoVO);

    /**
     * 保存处方
     *
     * @param recipeInfoVO 处方信息
     * @return
     */
    Integer saveRecipe(RecipeInfoVO recipeInfoVO);

    /**
     * 写死e签宝签名调用
     *
     * @param recipeInfoVO
     * @return
     */
    Integer esignRecipeCa(Integer recipeInfoVO);

    void fastRecipeCa(Integer recipeId);

    /**
     * 更新复诊的处方号
     * @param clinicId
     * @param recipeId
     */
    void updateRecipeIdByConsultId(Integer recipeId, Integer clinicId);

    /**
     * 是否有待处理处方
     * @param orderId
     * @return
     */
    ReadyRecipeVO getReadyRecipeFlag(Integer orderId);

    /**
     * 获取处方详情
     * @param patientRecipeDetailReq
     * @return
     */
    PatientRecipeDetailResVO patientRecipeDetail(PatientRecipeDetailReqVO patientRecipeDetailReq);
}
