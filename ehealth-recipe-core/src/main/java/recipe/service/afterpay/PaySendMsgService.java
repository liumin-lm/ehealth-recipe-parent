package recipe.service.afterpay;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.service.IRevisitService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;
import recipe.constant.PayConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.mq.Buss2SessionProducer;
import recipe.service.RecipeMsgService;

import java.util.List;

/**
 * @author yinsheng
 * @date 2021\4\13 0013 09:58
 */
@Component("paySendMsgService")
public class PaySendMsgService implements IAfterPayBussService{
    @Override
    public void handle(RecipeResultBean result, RecipeOrder recipeOrder, List<Recipe> recipes, Integer payFlag) {
        if (recipeOrder == null) {
            return;
        }
        if (RecipeResultBean.SUCCESS.equals(result.getCode()) && CollectionUtils.isNotEmpty(recipes)) {
            if (PayConstant.PAY_FLAG_PAY_SUCCESS == payFlag && 0 < recipeOrder.getActualPrice()) {
                RecipeMsgService.batchSendMsg(recipes.get(0), RecipeStatusConstant.HAVE_PAY);
            }
            //推送药品费用线上支付的处方单给复诊的医生
            if (PAY_MODE_ONLINE_TYPE.equals(recipeOrder.getPayMode())) {
                recipes.forEach(a->{
                    if (REVISIT_TYPE.equals(a.getBussSource()) && a.getClinicId() != null && RECIPE_SOURCE_ONLINE.equals(a.getRecipeSourceType())) {
                        IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
                        RevisitBean revisitBean = iRevisitService.getById(a.getClinicId());
                        if (revisitBean != null && REVISIT_STATUS_IN.equals(revisitBean.getStatus())) {
                            Buss2SessionProducer.sendMsgToMq(a, "recipePaySuccess", 4, revisitBean.getSessionID(), null);
                        }
                    }
                });
            }
        }
    }
}
