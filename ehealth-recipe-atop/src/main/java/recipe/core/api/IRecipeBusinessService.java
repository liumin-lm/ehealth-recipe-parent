package recipe.core.api;

import com.ngari.recipe.vo.OutPatientRecipeVO;

/**
 * @author yinsheng
 * @date 2021\7\16 0016 17:16
 */
public interface IRecipeBusinessService {

    /**
     * 查询线下门诊处方诊断信息
     *
     * @param organId     机构ID
     * @param patientName 患者名称
     * @param registerID  挂号序号
     * @param patientId   病历号
     * @return 诊断列表
     */
    String getOutRecipeDisease(Integer organId, String patientName, String registerID, String patientId);

    /**
     * 查询门诊处方信息
     * @param outPatientRecipeVO 患者信息
     */
    void queryOutPatientRecipe(OutPatientRecipeVO outPatientRecipeVO);
}
