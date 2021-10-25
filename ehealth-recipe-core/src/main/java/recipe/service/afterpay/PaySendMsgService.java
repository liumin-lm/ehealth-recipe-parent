package recipe.service.afterpay;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.service.IRevisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import recipe.constant.RecipeStatusConstant;
import recipe.mq.Buss2SessionProducer;
import recipe.service.RecipeMsgService;

import java.util.List;

/**
 * @author yinsheng
 * @date 2021\4\13 0013 09:58
 */
@Component("paySendMsgService")
public class PaySendMsgService implements IAfterPayBussService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaySendMsgService.class);

    /**
     * 发送支付后信息
     *
     * @param recipeOrder 订单信息
     * @param recipes     处方信息
     */
    public void sendPayMsg(RecipeOrder recipeOrder, List<Recipe> recipes) {
        LOGGER.info("PaySendMsgService sendPayMsg:[{}]", recipeOrder.getOrderCode());

        if (0 < recipeOrder.getActualPrice()) {
            RecipeMsgService.batchSendMsg(recipes.get(0), RecipeStatusConstant.HAVE_PAY);
        }
        recipes.forEach(a -> {
            //推送药品费用线上支付的处方单给复诊的医生
            if (PAY_MODE_ONLINE_TYPE.equals(recipeOrder.getPayMode())) {
                if (REVISIT_TYPE.equals(a.getBussSource()) && a.getClinicId() != null && RECIPE_SOURCE_ONLINE.equals(a.getRecipeSourceType())) {
                    IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
                    RevisitBean revisitBean = iRevisitService.getById(a.getClinicId());
                    if (revisitBean != null && REVISIT_STATUS_IN.equals(revisitBean.getStatus())) {
                        Buss2SessionProducer.sendMsgToMq(a, "recipePaySuccess", revisitBean.getSessionID());
                    }
                }
            }
            //推送给审方药师
            if (new Integer("1").equals(a.getRecipeCode())) {
                RecipeMsgService.batchSendMsg(a, RecipeStatusConstant.RECIPE_PAY_CALL_SUCCESS);
            }
        });

    }
}
