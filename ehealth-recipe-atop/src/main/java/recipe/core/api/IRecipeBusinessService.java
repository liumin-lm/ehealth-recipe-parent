package recipe.core.api;

import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.dto.OutPatientRecipeDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.vo.*;

import java.util.List;

/**
 * @author yinsheng
 * @date 2021\7\16 0016 17:16
 */
public interface IRecipeBusinessService {

    /**
     * 获取线下门诊处方诊断信息
     *
     * @param patientInfoVO 患者信息
     * @return 诊断列表
     */
    List<DiseaseInfoDTO> getOutRecipeDisease(PatientInfoVO patientInfoVO);

    /**
     * 查询门诊处方信息
     *
     * @param outPatientRecipeReqVO 患者信息
     */
    List<OutPatientRecipeDTO> queryOutPatientRecipe(OutPatientRecipeReqVO outPatientRecipeReqVO);

    /**
     * 获取门诊处方详情信息
     *
     * @param outRecipeDetailReqVO 门诊处方信息
     * @return 图片或者PDF链接等
     */
    OutRecipeDetailVO queryOutRecipeDetail(OutRecipeDetailReqVO outRecipeDetailReqVO);

    /**
     * 前端获取用药指导
     *
     * @param medicationGuidanceReqVO 用药指导入参
     * @return 用药指导出参
     */
    MedicationGuideResVO getMedicationGuide(MedicationGuidanceReqVO medicationGuidanceReqVO);

    /**
     * 根据处方来源，复诊id查询未审核处方个数
     *
     * @param bussSource 处方来源
     * @param clinicId   复诊Id
     * @return True存在 False不存在
     * @date 2021/7/20
     */
    Boolean existUncheckRecipe(Integer bussSource, Integer clinicId);

    /**
     * 获取处方信息
     *
     * @param recipeId 处方id
     * @return
     */
    Recipe getByRecipeId(Integer recipeId);

    /**
     * 校验开处方单数限制
     *
     * @param clinicId 复诊id
     * @param organId  机构id
     * @param recipeId 排除的处方id
     * @return true 可开方
     */
    Boolean validateOpenRecipeNumber(Integer clinicId, Integer organId, Integer recipeId);
}
