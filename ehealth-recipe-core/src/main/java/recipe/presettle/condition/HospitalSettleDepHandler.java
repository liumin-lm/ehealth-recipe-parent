package recipe.presettle.condition;

import org.springframework.stereotype.Component;
import recipe.presettle.RecipeOrderTypeEnum;
import recipe.presettle.model.OrderTypeCreateConditionRequest;

/**
 * created by shiyuping on 2020/11/30
 * 医院结算类型药企----省中模式
 * @author shiyuping
 */
@Component
public class HospitalSettleDepHandler implements IOrderTypeConditionHandler {

    /**医院结算类型药企----是否医院类型药企：1医院结算药企，0普通药企*/
    private final Integer HOSPITAL_SETTLE_DEP = 1;

    @Override
    public Integer getOrderType(OrderTypeCreateConditionRequest request) {
        if (request.getDrugsEnterprise() != null){
            Integer isHosDep = request.getDrugsEnterprise().getIsHosDep();
            if (HOSPITAL_SETTLE_DEP.equals(isHosDep)){
                return RecipeOrderTypeEnum.HOSPITAL_SELF.getType();
            }
        }
        return null;
    }

    @Override
    public int getPriorityLevel() {
        return 2;
    }
}
