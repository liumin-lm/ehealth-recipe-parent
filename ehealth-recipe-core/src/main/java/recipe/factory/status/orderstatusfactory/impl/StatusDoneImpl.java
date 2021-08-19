package recipe.factory.status.orderstatusfactory.impl;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.constant.PayConstant;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.hisservice.syncdata.SyncExecutorService;
import recipe.purchase.CommonOrder;

import java.util.Date;

/**
 * 已完成
 *
 * @author fuzi
 */
@Service
public class StatusDoneImpl extends AbstractRecipeOrderStatus {
    @Override
    public Integer getStatus() {
        return RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType();
    }

    @Override
    public Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder, Recipe recipe) {
        logger.info("StatusDoneImpl updateStatus orderStatus={},recipeOrder={}", JSON.toJSONString(orderStatus), JSON.toJSONString(recipeOrder));
        Date date = new Date();
        recipeOrder.setEffective(1);
        recipeOrder.setPayFlag(PayConstant.PAY_FLAG_PAY_SUCCESS);
        recipeOrder.setFinishTime(date);
        recipe.setGiveDate(date);
        recipe.setGiveFlag(1);
        recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_FINISH.getType());
        return recipe;
    }


    @Override
    public void upRecipeThreadPool(Recipe recipe) {
        logger.info("StatusDoneImpl upRecipeThreadPool recipe={}", JSON.toJSONString(recipe));
        Integer recipeId = recipe.getRecipeId();
        //更新pdf
        CommonOrder.finishGetDrugUpdatePdf(recipeId);
        //监管平台核销上传
        SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
        syncExecutorService.uploadRecipeVerificationIndicators(recipe.getRecipeId());
    }
}
