package recipe.factory.status.orderstatusfactory.impl;

import com.ngari.platform.recipe.mode.RecipeDrugInventoryDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.DrugStockClient;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.manager.PharmacyManager;
import recipe.thread.RecipeBusiThreadPool;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
    private DrugStockClient drugStockClient;
    @Autowired
    private CreatePdfFactory createPdfFactory;
    @Autowired
    private PharmacyManager pharmacyManager;

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
        Map<Integer, PharmacyTcm> pharmacyTcmMap = pharmacyManager.pharmacyIdMap(recipe.getClinicOrgan());
        RecipeDrugInventoryDTO request = drugStockClient.recipeDrugInventory(recipe, recipeDetailList, recipeOrderBill, pharmacyTcmMap);
        request.setInventoryType(DISPENSING_FLAG_DONE);
        drugStockClient.drugInventory(request);
        recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_DONE_DISPENSING.getType());
        return recipe;
    }

    @Override
    public void upRecipeThreadPool(Recipe recipe) {
        RecipeBusiThreadPool.execute(() -> createPdfFactory.updateGiveUser(recipe));
    }
}
