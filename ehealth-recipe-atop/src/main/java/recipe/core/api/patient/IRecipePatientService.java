package recipe.core.api.patient;

import com.ngari.recipe.vo.OutPatientReqVO;

/**
 * @author yinsheng
 * @date 2021\7\30 0030 09:52
 */
public interface IRecipePatientService {

    /**
     * 校验当前就诊人是否有效 是否实名认证 就诊卡是否有效
     * @param outPatientReqVO 当前就诊人信息
     * @return 枚举值
     */
    Integer checkCurrentPatient(OutPatientReqVO outPatientReqVO);
}
