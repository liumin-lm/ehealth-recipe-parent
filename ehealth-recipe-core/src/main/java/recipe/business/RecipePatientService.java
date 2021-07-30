package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.dto.HealthCardDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.vo.CheckPatientEnum;
import com.ngari.recipe.vo.OutPatientReqVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.HealthCardClient;
import recipe.client.PatientClient;
import recipe.core.api.patient.IRecipePatientService;

import java.util.Map;

/**
 * 处方患者服务
 * @author yinsheng
 * @date 2021\7\30 0030 09:53
 */
@Service
public class RecipePatientService extends BaseService implements IRecipePatientService{

    @Autowired
    private PatientClient patientClient;

    @Autowired
    private HealthCardClient healthCardClient;

    /**
     * 校验当前就诊人是否有效 是否实名认证 就诊卡是否有效
     * @param outPatientReqVO 当前就诊人信息
     * @return 枚举值
     */
    @Override
    public Integer checkCurrentPatient(OutPatientReqVO outPatientReqVO){
        logger.info("OutPatientRecipeService checkCurrentPatient outPatientReqVO:{}.", JSON.toJSONString(outPatientReqVO));
        PatientDTO patientDTO = patientClient.getPatientBeanByMpiId(outPatientReqVO.getMpiId());
        if (null == patientDTO || !new Integer(1).equals(patientDTO.getStatus())) {
            return CheckPatientEnum.CHECK_PATIENT_PATIENT.getType();
        }
        if (!new Integer(1).equals(patientDTO.getAuthStatus())) {
            return CheckPatientEnum.CHECK_PATIENT_NOAUTH.getType();
        }
        Map<String, HealthCardDTO> result = healthCardClient.findHealthCard(outPatientReqVO.getMpiId());
        if (null == result || (StringUtils.isNotEmpty(outPatientReqVO.getCardID()) && !result.containsKey(outPatientReqVO.getCardID()))) {
            return CheckPatientEnum.CHECK_PATIENT_CARDDEL.getType();
        }
        return CheckPatientEnum.CHECK_PATIENT_NORMAL.getType();
    }
}
