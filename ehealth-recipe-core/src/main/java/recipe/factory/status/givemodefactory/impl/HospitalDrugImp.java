package recipe.factory.status.givemodefactory.impl;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.stereotype.Service;
import recipe.enumerate.status.GiveModeEnum;
import recipe.enumerate.status.OrderStateEnum;
import recipe.enumerate.status.RecipeStateEnum;

/**
 * 医院取药
 *
 * @author fuzi
 */
@Service
public class HospitalDrugImp extends AbstractGiveMode {
    @Override
    public Integer getGiveMode() {
        return GiveModeEnum.GIVE_MODE_HOSPITAL_DRUG.getType();
    }

    @Override
    public void updateStatus(UpdateOrderStatusVO orderStatus) {
        orderStatus.setSender("");
        RecipeOrder recipeOrder = new RecipeOrder(orderStatus.getOrderId());
        Recipe recipe = super.getRecipe(orderStatus.getRecipeId());
        recipeOrder.setProcessState(OrderStateEnum.PROCESS_STATE_DISPENSING.getType());
        recipeOrder.setSubState(OrderStateEnum.SUB_DONE_SELF_TAKE.getType());
        recipe.setProcessState(RecipeStateEnum.PROCESS_STATE_DONE.getType());
        recipe.setSubState(RecipeStateEnum.SUB_DONE_SELF_TAKE.getType());
        recipeOrderStatusProxy.updateOrderByStatus(orderStatus, recipeOrder, recipe);
    }
}
