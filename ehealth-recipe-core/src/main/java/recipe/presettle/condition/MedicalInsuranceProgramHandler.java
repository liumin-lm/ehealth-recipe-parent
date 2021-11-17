package recipe.presettle.condition;

import com.alibaba.fastjson.JSONArray;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.ConsultBean;
import com.ngari.consult.common.service.IConsultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import recipe.presettle.RecipeOrderTypeEnum;
import recipe.presettle.model.OrderTypeCreateConditionRequest;

/**
 * created by shiyuping on 2020/12/4
 * 省中，邵逸夫医保小程序
 */
@Component
public class MedicalInsuranceProgramHandler implements IOrderTypeConditionHandler {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MedicalInsuranceProgramHandler.class);
    @Override
    public Integer getOrderType(OrderTypeCreateConditionRequest request) {
        LOGGER.info("MedicalInsuranceProgramHandler.getOrderType req={}", JSONArray.toJSONString(request));

        // 根据咨询单特殊来源标识和处方单特殊来源标识设置处方订单orderType为省中，邵逸夫医保小程序
        if (request.getRecipe() == null || null == request.getRecipe().getClinicId()) {
            return null;
        }
        ConsultBean consultBean = null;
        try {
            IConsultService consultService = ConsultAPI.getService(IConsultService.class);
            consultBean = consultService.getById(request.getRecipe().getClinicId());
        } catch (Exception e) {
            LOGGER.error("MedicalInsuranceProgramHandler error ", e);
        }
        LOGGER.info("MedicalInsuranceProgramHandler.getOrderType consultBean={}", JSONArray.toJSONString(consultBean));
        if (null != consultBean) {
            if (Integer.valueOf(1).equals(consultBean.getConsultSource()) && (Integer.valueOf(1).equals(request.getRecipe().getRecipeSource()))) {
                return RecipeOrderTypeEnum.PROVINCIAL_MEDICAL_APPLETS.getType();
            }
        }
        return null;
    }

    @Override
    public int getPriorityLevel() {
        return 0;
    }

}
