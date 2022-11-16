package recipe.presettle.condition;

import com.alibaba.fastjson.JSONArray;
import com.ngari.recipe.entity.OrganDrugsSaleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import recipe.dao.OrganDrugsSaleConfigDAO;
import recipe.presettle.RecipeOrderTypeEnum;
import recipe.presettle.model.OrderTypeCreateConditionRequest;

import java.util.Objects;

/**
 * created by shiyuping on 2020/11/30
 * 医院结算类型药企----省中模式
 * @author shiyuping
 */
@Component
public class HospitalSettleDepHandler implements IOrderTypeConditionHandler {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HospitalSettleDepHandler.class);

    @Autowired
    OrganDrugsSaleConfigDAO organDrugsSaleConfigDAO;

    @Override
    public Integer getOrderType(OrderTypeCreateConditionRequest request) {
        LOGGER.info("HospitalSettleDepHandler.getOrderType req={}", JSONArray.toJSONString(request));
        if (request.getDrugsEnterprise() != null) {
            OrganDrugsSaleConfig organDrugsSaleConfig = organDrugsSaleConfigDAO.getOrganDrugsSaleConfig(request.getDrugsEnterprise().getId());
            if (Objects.nonNull(organDrugsSaleConfig) && Integer.valueOf("1").equals(organDrugsSaleConfig.getIsHosDep())) {
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
