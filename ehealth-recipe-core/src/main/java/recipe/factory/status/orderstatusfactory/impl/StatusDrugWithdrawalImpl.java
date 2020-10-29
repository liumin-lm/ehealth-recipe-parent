package recipe.factory.status.orderstatusfactory.impl;

import com.ngari.platform.recipe.mode.RecipeDrugInventoryDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.stereotype.Service;
import recipe.constant.RecipeStatusConstant;
import recipe.factory.status.constant.RecipeOrderStatusEnum;

/**
 * 已退药
 *
 * @author fuzi
 */
@Service
public class StatusDrugWithdrawalImpl extends AbstractRecipeOrderStatus {
    @Override
    public Integer getStatus() {
        return RecipeOrderStatusEnum.ORDER_STATUS_DRUG_WITHDRAWAL.getType();
    }

    @Override
    public Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder) {
        RecipeDrugInventoryDTO request = hisInventoryClient.recipeDrugInventory(orderStatus.getRecipeId());
        request.setInventoryType(2);
        hisInventoryClient.drugInventory(request);
        recipeOrder.setEffective(EFFECTIVE);
        Recipe recipe = new Recipe();
        recipe.setRecipeId(orderStatus.getRecipeId());
        recipe.setStatus(RecipeStatusConstant.REVOKE);
        return recipe;
    }
}
