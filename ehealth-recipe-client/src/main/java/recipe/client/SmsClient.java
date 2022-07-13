package recipe.client;

import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.base.push.service.ISmsPushService;
import com.ngari.patient.dto.DoctorDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息通知
 *
 * @author yins
 */
@Service
public class SmsClient extends BaseClient {
    @Autowired
    private ISmsPushService smsPushService;

    public void pushMsgData2OnsExtendValue(Integer recipeId, Integer doctorId) {
        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
        SmsInfoBean smsInfo = new SmsInfoBean();
        smsInfo.setBusId(0);
        smsInfo.setOrganId(0);
        smsInfo.setBusType("DocSignNotify");
        smsInfo.setSmsType("DocSignNotify");
        smsInfo.setExtendValue(doctorDTO.getUrt() + "|" + recipeId + "|" + doctorDTO.getLoginId());
        smsPushService.pushMsgData2OnsExtendValue(smsInfo);
    }
}
