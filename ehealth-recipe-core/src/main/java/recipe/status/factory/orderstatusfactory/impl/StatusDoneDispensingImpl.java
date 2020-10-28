package recipe.status.factory.orderstatusfactory.impl;

import com.ngari.platform.recipe.mode.RecipeDrugInventoryDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.stereotype.Service;
import recipe.constant.RecipeStatusConstant;
import recipe.status.factory.constant.RecipeOrderStatusEnum;

/**
 * 已发药
 *
 * @author fuzi
 */
@Service
public class StatusDoneDispensingImpl extends AbstractRecipeOrderStatus {

    @Override
    public Integer getStatus() {
        return RecipeOrderStatusEnum.ORDER_STATUS_DONE_DISPENSING.getType();
    }

    @Override
    public Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder) {
        RecipeDrugInventoryDTO request = super.recipeDrugInventory(orderStatus.getRecipeId());
        request.setInventoryType(1);
        super.drugInventory(request);
        Recipe recipe = new Recipe();
        recipe.setRecipeId(orderStatus.getRecipeId());
        recipe.setStatus(RecipeStatusConstant.WAIT_SEND);
        recipeDAO.updateNonNullFieldByPrimaryKey(recipe);
        return null;
    }
}
