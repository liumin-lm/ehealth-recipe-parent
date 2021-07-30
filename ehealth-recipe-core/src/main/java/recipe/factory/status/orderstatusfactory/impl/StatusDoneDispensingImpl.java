package recipe.factory.status.orderstatusfactory.impl;

import com.ngari.platform.recipe.mode.RecipeDrugInventoryDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.RecipeOrderBill;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.HisInventoryClient;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.thread.RecipeBusiThreadPool;

import java.util.Date;
import java.util.List;

/**
 * 已发药
 *
 * @author fuzi
 */
@Service
public class StatusDoneDispensingImpl extends AbstractRecipeOrderStatus {
    /**
     * 发药标示：0:无需发药，1：已发药，2:已退药
     */
    protected final static int DISPENSING_FLAG_DONE = 1;
    @Autowired
    private HisInventoryClient hisInventoryClient;
    @Autowired
    private CreatePdfFactory createPdfFactory;

    @Override
    public Integer getStatus() {
        return RecipeOrderStatusEnum.ORDER_STATUS_DONE_DISPENSING.getType();
    }

    @Override
    public Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder, Recipe recipe) {
        recipeOrder.setDispensingFlag(DISPENSING_FLAG_DONE);
        recipeOrder.setDispensingTime(new Date());
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        RecipeOrderBill recipeOrderBill = recipeOrderBillDAO.getRecipeOrderBillByOrderCode(recipe.getOrderCode());
        RecipeDrugInventoryDTO request = hisInventoryClient.recipeDrugInventory(recipe, recipeDetailList, recipeOrderBill);
        request.setInventoryType(DISPENSING_FLAG_DONE);
        hisInventoryClient.drugInventory(request);
        recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_DONE_DISPENSING.getType());
        return recipe;
    }

    @Override
    public void upRecipeThreadPool(Recipe recipe) {
        RecipeBusiThreadPool.execute(() -> createPdfFactory.updateGiveUser(recipe));
    }
}
