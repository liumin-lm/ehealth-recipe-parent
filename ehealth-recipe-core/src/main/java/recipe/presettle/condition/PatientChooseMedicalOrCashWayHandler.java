package recipe.presettle.condition;

import com.alibaba.fastjson.JSONArray;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.HealthCardService;
import com.ngari.patient.service.OrganService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import recipe.ApplicationUtils;
import recipe.constant.RecipeBussConstant;
import recipe.presettle.RecipeOrderTypeEnum;
import recipe.presettle.model.OrderTypeCreateConditionRequest;
import recipe.purchase.PayModeOnline;

/**
 * created by shiyuping on 2020/11/30
 * 患者购药选择的支付方式
 *
 * @author shiyuping
 */
@Component
public class PatientChooseMedicalOrCashWayHandler implements IOrderTypeConditionHandler {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientChooseMedicalOrCashWayHandler.class);

    @Override
    public Integer getOrderType(OrderTypeCreateConditionRequest request) {
        LOGGER.info("PatientChooseMedicalOrCashWayHandler.getOrderType req={}", JSONArray.toJSONString(request));
        if (request.getRecipeOrder() != null) {
            Integer orderType = request.getRecipeOrder().getOrderType();
            if (orderType == null) {
                return RecipeOrderTypeEnum.COMMON_SELF.getType();
            }
            //目前除了医院结算药企的配置之外没有自费预结算
            //这次发版后杭州市医保会修改配置前端没有医保按钮只有提交订单
            //也就是患者选择了医保肯定就是省直医保的类型
            if (RecipeBussConstant.ORDERTYPE_ZJS.equals(orderType)) {
                return RecipeOrderTypeEnum.PROVINCIAL_MEDICAL.getType();
            } else {
                //根据患者的绑定的市名卡判断是杭州市医保还是普通自费
                //获取杭州市市民卡
                HealthCardService healthCardService = ApplicationUtils.getBasicService(HealthCardService.class);
                //杭州市互联网医院监管中心 管理单元eh3301
                OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
                OrganDTO organDTO = organService.getByManageUnit("eh3301");
                LOGGER.info("PatientChooseMedicalOrCashWayHandler.getOrderType organDTO={}", JSONArray.toJSONString(organDTO));
                String bxh = null;
                if (organDTO != null) {
                    bxh = healthCardService.getMedicareCardId(request.getRecipeOrder().getMpiId(), organDTO.getOrganId());
                }
                LOGGER.info("PatientChooseMedicalOrCashWayHandler.getOrderType bxh={}", bxh);
                if (StringUtils.isNotEmpty(bxh)) {
                    //杭州市医保
                    return RecipeOrderTypeEnum.HANGZHOU_MEDICAL.getType();
                } else {
                    //普通自费
                    return RecipeOrderTypeEnum.COMMON_SELF.getType();
                }
            }
        }
        return null;
    }

    @Override
    public int getPriorityLevel() {
        return 3;
    }
}
