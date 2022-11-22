package recipe.factory.status.orderstatusfactory.impl;

import com.ngari.platform.recipe.mode.RecipeDrugInventoryDTO;
import com.ngari.recipe.drug.model.OrganDrugListBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.DrugStockClient;
import recipe.constant.ErrorCode;
import recipe.enumerate.status.OrderStateEnum;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.manager.PharmacyManager;
import recipe.service.OrganDrugListService;
import recipe.thread.RecipeBusiThreadPool;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Autowired
    private OrganDrugListService organDrugListService;

    @Override
    public Integer getStatus() {
        return RecipeOrderStatusEnum.ORDER_STATUS_DONE_DISPENSING.getType();
    }

    @Override
    public Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder, Recipe recipe) {
        recipeOrder.setDispensingFlag(DISPENSING_FLAG_DONE);
        recipeOrder.setDispensingTime(new Date());
        RecipeOrder order = recipeOrderDAO.get(recipeOrder.getOrderId());
        List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeIdList(recipeIdList);
        try {
            for(Recipedetail recipedetail : recipeDetailList){
                OrganDrugListBean organDrugList = organDrugListService.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), recipedetail.getOrganDrugCode(), recipedetail.getDrugId());
                logger.info("StatusDoneDispensingImpl updateStatus  organDrugList={}", JSONUtils.toString(organDrugList));
                if(null != organDrugList){
                    recipedetail.setDrugItemCode(organDrugList.getDrugItemCode());
                }
            }
        }catch (Exception e){
            logger.error("StatusDoneDispensingImpl updateStatus  error",e);
        }
        drugInventory(recipeIdList,recipeDetailList,recipe);
        recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_DONE_DISPENSING.getType());
        recipeOrder.setProcessState(OrderStateEnum.PROCESS_STATE_DISPENSING.getType());
        recipeOrder.setSubState(OrderStateEnum.SUB_DONE_SELF_TAKE.getType());
        recipe.setProcessState(RecipeStateEnum.PROCESS_STATE_DONE.getType());
        recipe.setSubState(RecipeStateEnum.SUB_DONE_SELF_TAKE.getType());
        return recipe;
    }

    @Override
    public void upRecipeThreadPool(Recipe recipe) {
        RecipeBusiThreadPool.execute(() -> createPdfFactory.updateGiveUser(recipe));
    }

    private void drugInventory(List<Integer> recipeIdList,List<Recipedetail> recipeDetailList,Recipe recipe){
        if(CollectionUtils.isEmpty(recipeDetailList)){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品列表为空");
        }
        Map<Integer, List<Recipedetail>> collect = recipeDetailList.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIdList);
        RecipeOrderBill recipeOrderBill = recipeOrderBillDAO.getRecipeOrderBillByOrderCode(recipe.getOrderCode());
        Map<Integer, PharmacyTcm> pharmacyTcmMap = pharmacyManager.pharmacyIdMap(recipe.getClinicOrgan());
        recipes.forEach(r -> {
            List<Recipedetail> recipeDetails = collect.get(r.getRecipeId());
            RecipeDrugInventoryDTO request = drugStockClient.recipeDrugInventory(r, recipeDetails, recipeOrderBill, pharmacyTcmMap);
            request.setInventoryType(DISPENSING_FLAG_DONE);
            drugStockClient.drugInventory(request);
        });
    }
}
