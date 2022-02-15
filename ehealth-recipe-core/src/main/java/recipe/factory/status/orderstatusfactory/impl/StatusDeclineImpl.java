package recipe.factory.status.orderstatusfactory.impl;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.stereotype.Service;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;

/**
 * 已拒发
 *
 * @author fuzi
 */
@Service
public class StatusDeclineImpl extends AbstractRecipeOrderStatus {
    @Override
    public Integer getStatus() {
        return RecipeOrderStatusEnum.ORDER_STATUS_DECLINE.getType();
    }

    @Override
    public Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder, Recipe recipe) {
        recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_DECLINE.getType());
        stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_REFUSE_ORDER);
        return recipe;
    }
}
