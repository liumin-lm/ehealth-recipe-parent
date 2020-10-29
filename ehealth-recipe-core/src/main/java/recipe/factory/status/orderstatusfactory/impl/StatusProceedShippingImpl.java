package recipe.factory.status.orderstatusfactory.impl;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.common.response.CommonResponse;
import recipe.constant.RecipeStatusConstant;
import recipe.factory.status.constant.RecipeOrderStatusEnum;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.thread.RecipeBusiThreadPool;

import java.util.Date;

/**
 * 配送中
 *
 * @author fuzi
 */
@Service
public class StatusProceedShippingImpl extends AbstractRecipeOrderStatus {
    @Override
    public Integer getStatus() {
        return RecipeOrderStatusEnum.ORDER_STATUS_PROCEED_SHIPPING.getType();
    }

    @Override
    public Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder) {
        logger.info("StatusProceedShippingImpl updateStatus orderStatus={},recipeOrder={}",
                JSON.toJSONString(orderStatus), JSON.toJSONString(recipeOrder));

        Date date = new Date();
        Integer recipeId = orderStatus.getRecipeId();
        Recipe recipe = super.getRecipe(recipeId);
        recipe.setSendDate(date);
        recipe.setSender(orderStatus.getSender());
        //以免进行处方失效前提醒
        recipe.setRemindFlag(1);
        recipe.setStatus(RecipeStatusConstant.IN_SEND);
        recipeOrder.setSendTime(new Date());
        recipeOrder.setOrderId(orderStatus.getOrderId());
        if (null != orderStatus.getLogisticsCompany()) {
            recipeOrder.setLogisticsCompany(orderStatus.getLogisticsCompany());
        }
        if (StringUtils.isNotEmpty(orderStatus.getTrackingNumber())) {
            recipeOrder.setTrackingNumber(orderStatus.getTrackingNumber());
        }
        return recipe;
    }

    @Override
    public void upRecipeThreadPool(Recipe recipe) {
        logger.info("StatusProceedShippingImpl upRecipeThreadPool recipe={}", JSON.toJSONString(recipe));
        //监管平台上传配送信息(派药)
        RecipeBusiThreadPool.execute(() -> {
            //HIS消息发送
            RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.IN_SEND);
            HisSyncSupervisionService hisSyncService = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
            CommonResponse response = hisSyncService.uploadSendMedicine(recipe.getRecipeId());
            //记录日志
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), RecipeStatusConstant.IN_SEND,
                    "监管平台配送信息[派药]上传code" + response.getCode() + ",msg:" + response.getMsg());
        });
    }
}
