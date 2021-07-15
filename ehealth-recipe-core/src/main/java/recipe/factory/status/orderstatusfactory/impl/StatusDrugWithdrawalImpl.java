package recipe.factory.status.orderstatusfactory.impl;

import com.ngari.platform.recipe.mode.RecipeDrugInventoryDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.RecipeOrderBill;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.HisInventoryClient;
import recipe.factory.status.constant.RecipeOrderStatusEnum;
import recipe.factory.status.constant.RecipeStatusEnum;

import java.util.List;

/**
 * 已退药
 *
 * @author fuzi
 */
@Service
public class StatusDrugWithdrawalImpl extends AbstractRecipeOrderStatus {
    /**
     * 发药标示：0:无需发药，1：已发药，2:已退药
     */
    protected final static int DISPENSING_FLAG_WITHDRAWAL = 2;
    @Autowired
    private HisInventoryClient hisInventoryClient;

    @Override
    public Integer getStatus() {
        return RecipeOrderStatusEnum.ORDER_STATUS_DRUG_WITHDRAWAL.getType();
    }

    @Override
    public Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder, Recipe recipe) {
        recipeOrder.setDispensingFlag(DISPENSING_FLAG_WITHDRAWAL);
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        RecipeOrderBill recipeOrderBill = recipeOrderBillDAO.getRecipeOrderBillByOrderCode(recipe.getOrderCode());
        RecipeDrugInventoryDTO request = hisInventoryClient.recipeDrugInventory(recipe, recipeDetailList, recipeOrderBill);
        request.setInventoryType(DISPENSING_FLAG_WITHDRAWAL);
        hisInventoryClient.drugInventory(request);
        recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_DRUG_WITHDRAWAL.getType());
        return recipe;
    }
    
}
