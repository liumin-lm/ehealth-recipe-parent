package recipe.presettle.condition;

import com.ngari.consult.ConsultAPI;
import com.ngari.consult.ConsultBean;
import com.ngari.consult.common.service.IConsultService;
import org.springframework.stereotype.Component;
import recipe.presettle.RecipeOrderTypeEnum;
import recipe.presettle.model.OrderTypeCreateConditionRequest;

/**
 * created by shiyuping on 2020/12/4
 * 省中，邵逸夫医保小程序
 */
@Component
public class MedicalInsuranceProgramHandler implements IOrderTypeConditionHandler {
    @Override
    public Integer getOrderType(OrderTypeCreateConditionRequest request) {
        // 根据咨询单特殊来源标识和处方单特殊来源标识设置处方订单orderType为省中，邵逸夫医保小程序
        if (request.getRecipe() == null || null == request.getRecipe().getClinicId()) {
            return null;
        }
        IConsultService consultService = ConsultAPI.getService(IConsultService.class);
        ConsultBean consultBean = consultService.getById(request.getRecipe().getClinicId());
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
