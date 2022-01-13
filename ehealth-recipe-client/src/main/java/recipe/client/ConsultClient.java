package recipe.client;

import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.util.ValidateUtil;

/**
 * 咨询相关服务
 *
 * @Author liumin
 * @Date 2022/1/10 下午2:26
 * @Description
 */
@Service
public class ConsultClient extends BaseClient {

    @Autowired
    private IConsultExService consultExService;

    /**
     * 根据单号获取网络门诊信息
     *
     * @param clinicId 业务单号
     * @return 网络门诊信息
     */
    public ConsultExDTO getConsultExByClinicId(Integer clinicId) {
        if (ValidateUtil.integerIsEmpty(clinicId)) {
            return null;
        }
        logger.info("ConsultClient getByClinicId param clinicId:{}", clinicId);
        ConsultExDTO consultExDTO = consultExService.getByConsultId(clinicId);
        logger.info("ConsultClient getByClinicId res consultExDTO:{} ", JSONUtils.toString(consultExDTO));
        return consultExDTO;
    }

}
