package recipe.factory.status.givemodefactory.impl;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.constant.RecipeStatusConstant;
import recipe.factory.status.constant.GiveModeEnum;
import recipe.factory.status.constant.RecipeOrderStatusEnum;
import recipe.hisservice.syncdata.SyncExecutorService;
import recipe.service.RecipeHisService;
import recipe.service.RecipeMsgService;

import java.util.Date;

/**
 * 药店取药
 *
 * @author fuzi
 */
public class PharmacyDrugImpl extends AbstractGiveMode {
    @Autowired
    private RecipeHisService recipeHisService;

    @Override
    public Integer getGiveMode() {
        return GiveModeEnum.GIVE_MODE_PHARMACY_DRUG.getType();
    }

    @Override
    public void updateStatus(UpdateOrderStatusVO orderStatus) {
        orderStatus.setSender("");
        RecipeOrder recipeOrder = new RecipeOrder(orderStatus.getOrderId());
        recipeOrder.setPayTime(new Date());
        Recipe recipe = super.getRecipe(orderStatus.getRecipeId());
        recipeOrderStatusProxy.updateOrderByStatus(orderStatus, recipeOrder, recipe);
    }

    @Override
    public void updateStatusAfter(UpdateOrderStatusVO orderStatus) {
        Integer recipeId = orderStatus.getRecipeId();
        if (RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType().equals(orderStatus.getTargetRecipeOrderStatus())) {
            //HIS消息发送
            recipeHisService.recipeFinish(recipeId);
            //发送取药完成消息
            RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.RECIPE_TAKE_MEDICINE_FINISH);
            //监管平台核销上传
            SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
            syncExecutorService.uploadRecipeVerificationIndicators(recipeId);
        }
    }

}
