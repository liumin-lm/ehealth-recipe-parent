package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.base.push.service.ISmsPushService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.recipe.entity.Recipe;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

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
        try {
            DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
            SmsInfoBean smsInfo = new SmsInfoBean();
            smsInfo.setBusId(0);
            smsInfo.setOrganId(0);
            smsInfo.setBusType("DocSignNotify");
            smsInfo.setSmsType("DocSignNotify");
            smsInfo.setExtendValue(doctorDTO.getUrt() + "|" + recipeId + "|" + doctorDTO.getLoginId());
            smsPushService.pushMsgData2OnsExtendValue(smsInfo);
            logger.info("SmsClient pushMsgData2OnsExtendValue smsInfo = {}", JSON.toJSONString(smsInfo));
        } catch (Exception e) {
            logger.info("SmsClient pushMsgData2OnsExtendValue recipeId = {},doctorId={}", recipeId, doctorId, e);
        }

    }

    public void pushMsgData2OnsExtendValue(SmsInfoBean smsInfoBean) {
        smsPushService.pushMsgData2OnsExtendValue(smsInfoBean);
    }

    /**
     * 推送消息
     *
     * @param recipe
     * @param busType
     * @param smsMap
     */
    public void pushSmsInfo(Recipe recipe, String busType, Map<String, Object> smsMap) {
        SmsInfoBean smsInfo = new SmsInfoBean();
        smsInfo.setBusType(busType);
        smsInfo.setSmsType(busType);
        smsInfo.setBusId(recipe.getRecipeId());
        smsInfo.setOrganId(recipe.getClinicOrgan());
        smsInfo.setExtendValue(JSONUtils.toString(smsMap));
        logger.info("SmsClient smsInfo:{}", JSON.toJSONString(smsInfo));
        smsPushService.pushMsgData2OnsExtendValue(smsInfo);
    }

    /**
     * 便捷够药给患者发送消息
     *
     * @param recipe
     */
    public void patientConvenientDrug(Recipe recipe) {
        SmsInfoBean smsInfoBean = new SmsInfoBean();
        smsInfoBean.setBusType("FastRecipeApplySuccess");
        smsInfoBean.setSmsType("FastRecipeApplySuccess");
        smsInfoBean.setBusId(recipe.getRecipeId());
        smsInfoBean.setOrganId(recipe.getClinicOrgan());
        this.pushMsgData2OnsExtendValue(smsInfoBean);
    }

    /**
     * 便捷购药手动开方通知医生
     *
     * @param organId
     * @param doctorId
     */
    public void fastRecipeApplyToDoctor(Integer organId, Integer doctorId) {
        SmsInfoBean smsInfoBean = new SmsInfoBean();
        smsInfoBean.setBusType("fastRecipeApplyToDoctor");
        smsInfoBean.setSmsType("fastRecipeApplyToDoctor");
        smsInfoBean.setBusId(doctorId);
        smsInfoBean.setOrganId(organId);
        this.pushMsgData2OnsExtendValue(smsInfoBean);
    }


    public void therapyRecipeApplyToPatient(Recipe recipe) {
        SmsInfoBean smsInfoBean = new SmsInfoBean();
        smsInfoBean.setBusType("therapyRecipeApply");
        smsInfoBean.setSmsType("therapyRecipeApply");
        smsInfoBean.setBusId(recipe.getRecipeId());
        smsInfoBean.setOrganId(recipe.getClinicOrgan());
        this.pushMsgData2OnsExtendValue(smsInfoBean);
    }
}
