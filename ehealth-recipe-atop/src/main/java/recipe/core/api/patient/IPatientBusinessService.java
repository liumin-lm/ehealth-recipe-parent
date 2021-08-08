package recipe.core.api.patient;

import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.vo.OutPatientReqVO;
import com.ngari.recipe.vo.PatientInfoVO;
import com.ngari.recipe.vo.PatientMedicalTypeVO;

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
     * 根据mpiId获取患者信息
     * @param mpiId 患者唯一号
     * @return 患者信息
     */
    PatientDTO getPatientDTOByMpiID(String mpiId);

    /**
     * 获取患者医保信息
     * @param patientInfoVO 患者信息
     * @return 医保类型相关
     */
    PatientMedicalTypeVO queryPatientMedicalType(PatientInfoVO patientInfoVO);
}
