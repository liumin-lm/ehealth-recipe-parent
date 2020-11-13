package recipe.factory.status.orderstatusfactory.impl;

import com.ngari.platform.recipe.mode.RecipeDrugInventoryDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.factory.status.constant.RecipeOrderStatusEnum;
import recipe.factory.status.constant.RecipeStatusEnum;
import recipe.service.client.HisInventoryClient;

/**
 * 已退药
 *
 * @author fuzi
 */
@Service
public class StatusDrugWithdrawalImpl extends AbstractRecipeOrderStatus {
    @Autowired
    private HisInventoryClient hisInventoryClient;

    @Override
    public Integer getStatus() {
        return RecipeOrderStatusEnum.ORDER_STATUS_DRUG_WITHDRAWAL.getType();
    }

    @Override
    public Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder) {
        recipeOrder.setEffective(EFFECTIVE);
        recipeOrder.setDispensingFlag(2);
        RecipeDrugInventoryDTO request = hisInventoryClient.recipeDrugInventory(orderStatus.getRecipeId());
        request.setInventoryType(2);
        hisInventoryClient.drugInventory(request);
        Recipe recipe = new Recipe();
        recipe.setRecipeId(orderStatus.getRecipeId());
        recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_DRUG_WITHDRAWAL.getType());
        return recipe;
    }
    
}
