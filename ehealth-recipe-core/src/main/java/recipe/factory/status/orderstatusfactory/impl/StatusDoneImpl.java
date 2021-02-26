package recipe.factory.status.orderstatusfactory.impl;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.stereotype.Service;
import recipe.constant.PayConstant;
import recipe.constant.RecipeBussConstant;
import recipe.factory.status.constant.RecipeOrderStatusEnum;
import recipe.factory.status.constant.RecipeStatusEnum;
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
    public Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder) {
        logger.info("StatusDoneImpl updateStatus orderStatus={},recipeOrder={}", JSON.toJSONString(orderStatus), JSON.toJSONString(recipeOrder));
        Integer recipeId = orderStatus.getRecipeId();
        Recipe recipe = super.getRecipe(recipeId);
        Date date = new Date();
        recipeOrder.setEffective(1);
        recipeOrder.setPayFlag(PayConstant.PAY_FLAG_PAY_SUCCESS);
        recipeOrder.setFinishTime(date);
        //如果是货到付款还要更新付款时间和付款状态
        if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode()) && RecipeBussConstant.PAYMODE_OFFLINE.equals(recipeOrder.getPayMode())) {
            recipeOrder.setPayTime(date);
            recipe.setPayFlag(1);
            recipe.setPayDate(date);
        }
        recipe.setRecipeId(recipe.getRecipeId());
        recipe.setGiveDate(date);
        recipe.setGiveFlag(1);
        recipe.setGiveUser(orderStatus.getSender());
        recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_FINISH.getType());
        return recipe;
    }


    @Override
    public void upRecipeThreadPool(Recipe recipe) {
        logger.info("StatusDoneImpl upRecipeThreadPool recipe={}", JSON.toJSONString(recipe));
        Integer recipeId = recipe.getRecipeId();
        //更新pdf
        CommonOrder.finishGetDrugUpdatePdf(recipeId);
    }
}
