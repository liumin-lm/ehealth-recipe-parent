package recipe.manager;

import com.alibaba.fastjson.JSONArray;
import com.ngari.base.BaseAPI;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import ctd.account.Client;
import eh.base.constant.CardTypeEnum;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Service;
import recipe.client.IConfigurationClient;
import recipe.enumerate.type.PayButtonEnum;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @description： 按钮 manager
 * @author： whf
 * @date： 2021-08-19 9:38
 */
@Service
public class ButtonManager extends BaseManager {

    @Resource
    private IConfigurationClient configurationClient;
    @Resource
    private IConfigurationCenterUtilsService configService;

    /**
     * 医保支付 key
     */
    private static final String medicalPayConfigKey = "medicalPayConfig";
    private static final String provincialMedicalPayFlagKey = "provincialMedicalPayFlag";
    private static final String technicalSupportKey = "technicalSupport";

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
        Integer technicalSupport = configurationClient.getPropertyByClientId(technicalSupportKey);
        Integer valueCatch = configurationClient.getValueCatch(organId, provincialMedicalPayFlagKey, 0);
        Boolean valueBooleanCatch = configurationClient.getValueBooleanCatch(technicalSupport, medicalPayConfigKey, false);
        logger.info("ButtonManager.getPayButton technicalSupport={} valueCatch={} valueBooleanCatch={}",technicalSupport, valueCatch,valueBooleanCatch);

        // medicalPayConfig = true +  provincialMedicalPayFlag > 1 + 市民卡  展示医保支付
        if (valueBooleanCatch && valueCatch > 1 && CardTypeEnum.INSURANCECARD.getValue().equals(cardType)) {
            return PayButtonEnum.MEDICAL_PAY.getType();
        }
        return PayButtonEnum.MY_PAY.getType();
    }
}
