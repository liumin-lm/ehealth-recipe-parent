package recipe.manager;

import eh.base.constant.CardTypeEnum;
import org.springframework.stereotype.Service;
import recipe.client.IConfigurationClient;
import recipe.enumerate.type.PayButtonEnum;

import javax.annotation.Resource;

/**
 * @description： 按钮 manager
 * @author： whf
 * @date： 2021-08-19 9:38
 */
@Service
public class ButtonManager extends BaseManager {
    @Resource
    private IConfigurationClient configurationClient;

    private static final String medicalPayConfigKey = "medicalPayConfig";
    private static final String provincialMedicalPayFlagKey = "provincialMedicalPayFlag";

    /**
     * 获取支付按钮 仅杭州市互联网医院使用
     *
     * @param organId  机构id
     * @param cardType 患者卡类型
     * @param isForce  是否强制自费
     * @return
     */
    public Integer getPayButton(Integer organId, String cardType, Boolean isForce) {
        logger.info("ButtonManager.getPayButton req organId={} cardType={} isForce={}",organId,cardType,isForce);

        // 强制自费 + 健康卡 展示 自费支付
        if (isForce) {
            return PayButtonEnum.MY_PAY.getType();
        }
        Boolean valueBooleanCatch = configurationClient.getPropertyByClientId(medicalPayConfigKey);
        Integer valueCatch = configurationClient.getValueCatchReturnInteger(organId, provincialMedicalPayFlagKey, 0);
        logger.info("ButtonManager.getPayButton  valueCatch={} valueBooleanCatch={}", valueCatch, valueBooleanCatch);
        if (valueBooleanCatch && valueCatch > 1 && CardTypeEnum.INSURANCECARD.getValue().equals(cardType)) {
            return PayButtonEnum.MEDICAL_PAY.getType();
        }
        return PayButtonEnum.MY_PAY.getType();
    }
}
