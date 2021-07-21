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
     *
     * @param outPatientRecipeVO 患者信息
     */
    void queryOutPatientRecipe(OutPatientRecipeVO outPatientRecipeVO);

    /**
     * @Description: 根据处方来源，复诊id查询未审核处方个数
     * @Param: bussSource 处方来源
     * @Param: clinicId 复诊Id
     * @return: True存在 False不存在
     * @Date: 2021/7/20
     */
    Boolean existUncheckRecipe(Integer bussSource, Integer clinicId);
}
