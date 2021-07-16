package recipe.core.api.patient;

/**
 * @author yinsheng
 * @date 2021\7\16 0016 17:16
 */
public interface IOutPatientRecipeService {

    /**
     * 查询线下门诊处方诊断信息
     * @param organId 机构ID
     * @param patientName 患者名称
     * @param registerID 挂号序号
     * @param patientId 病历号
     * @return  诊断列表
     */
    String getOutRecipeDisease(Integer organId, String patientName, String registerID, String patientId);
}
