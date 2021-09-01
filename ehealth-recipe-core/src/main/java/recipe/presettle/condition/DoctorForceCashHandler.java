package recipe.presettle.condition;

import com.alibaba.fastjson.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import recipe.presettle.RecipeOrderTypeEnum;
import recipe.presettle.model.OrderTypeCreateConditionRequest;

/**
 * created by shiyuping on 2020/11/27
 * 医生强制自费，不能强制医保
 * @author shiyuping
 */
@Component
public class DoctorForceCashHandler implements IOrderTypeConditionHandler {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DoctorForceCashHandler.class);
    /**医生选择的自费类型*/
    private final String MEDICALTYPE_ZF = "0";

    @Override
    public Integer getOrderType(OrderTypeCreateConditionRequest request) {
        LOGGER.info("DoctorForceCashHandler.getOrderType req={}", JSONArray.toJSONString(request));
        if (request.getRecipeExtend() != null){
            String medicalType = request.getRecipeExtend().getMedicalType();
            if (StringUtils.isEmpty(medicalType)){
                return null;
            }
            //有医生选择的医保还是自费的功能时才返回具体预结算
            //医生强制自费后就结束
            //医生强制医保还得看患者是否选择自费
            if (MEDICALTYPE_ZF.equals(medicalType)){
                return RecipeOrderTypeEnum.COMMON_SELF.getType();
            }
        }
        return null;
    }

    @Override
    public int getPriorityLevel() {
        return 1;
    }
}
