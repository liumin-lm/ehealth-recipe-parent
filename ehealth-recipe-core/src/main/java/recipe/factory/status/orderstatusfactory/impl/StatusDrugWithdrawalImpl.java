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
import recipe.client.DrugStockClient;
import recipe.constant.ErrorCode;
import recipe.enumerate.status.OrderStateEnum;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.manager.PharmacyManager;
import recipe.service.OrganDrugListService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private DrugStockClient drugStockClient;
    @Autowired
    private PharmacyManager pharmacyManager;
    @Autowired
    private OrganDrugListService organDrugListService;

    @Override
    public Integer getStatus() {
        return RecipeOrderStatusEnum.ORDER_STATUS_DRUG_WITHDRAWAL.getType();
    }

    @Override
    public Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder, Recipe recipe) {
        recipeOrder.setDispensingFlag(DISPENSING_FLAG_WITHDRAWAL);
        RecipeOrder order = recipeOrderDAO.get(recipeOrder.getOrderId());
        List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeIdList(recipeIdList);
        try {
            for(Recipedetail recipedetail : recipeDetailList){
                OrganDrugListBean organDrugList = organDrugListService.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getRecipeId(), recipedetail.getOrganDrugCode(), recipedetail.getDrugId());
                logger.info("StatusDrugWithdrawalImpl updateStatus  organDrugList={}", JSONUtils.toString(organDrugList));
                if(null != organDrugList){
                    recipedetail.setDrugItemCode(organDrugList.getDrugItemCode());
                }
            }
        }catch (Exception e){
            logger.info("StatusDrugWithdrawalImpl updateStatus  error",e);
        }
        drugInventory(recipeIdList,recipeDetailList,recipe);
        recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_DRUG_WITHDRAWAL.getType());
        recipe.setProcessState(RecipeStateEnum.PROCESS_STATE_CANCELLATION.getType());
        recipe.setSubState(RecipeStateEnum.SUB_CANCELLATION_RETURN_DRUG.getType());
        stateManager.updateOrderState(recipeOrder.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_RETURN_DRUG);
        stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.getRecipeStateEnum(recipe.getProcessState()), RecipeStateEnum.getRecipeStateEnum(recipe.getSubState()));
        return recipe;
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
            request.setInventoryType(DISPENSING_FLAG_WITHDRAWAL);
            drugStockClient.drugInventory(request);
        });
    }
    
}
