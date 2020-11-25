package recipe.drugsenterprise.paymodeonlineshowdep;

import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.RecipeBussConstant;
import recipe.service.RecipeOrderService;

import java.util.List;

/**
 * created by shiyuping on 2020/11/10
 * @author shiyuping
 */
public class CommonShowDepService implements PayModeOnlineShowDepInterface {
    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CommonShowDepService.class);


    @Override
    public void getPayModeOnlineShowDep(DrugsEnterprise dep, List<DepDetailBean> depDetailList, Recipe dbRecipe, List<Integer> recipeIdList) {
        DepDetailBean depDetailBean = new DepDetailBean();
        depDetailBean.setDepId(dep.getId());
        depDetailBean.setDepName(dep.getName());
        depDetailBean.setRecipeFee(dbRecipe.getTotalMoney());
        depDetailBean.setBelongDepName(dep.getName());
        depDetailBean.setOrderType(dep.getOrderType());
        depDetailBean.setMemo(dep.getMemo());
        if (RecipeBussConstant.PAYMODE_ONLINE.equals(dep.getPayModeSupport()) || RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS.equals(dep.getPayModeSupport())) {
            depDetailBean.setPayModeText("在线支付");
            depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_ONLINE);
        } else {
            depDetailBean.setPayModeText("货到付款");
            depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_COD);
        }

        RecipeOrderService recipeOrderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        //重置药企处方价格
        depDetailBean.setRecipeFee(recipeOrderService.reCalculateRecipeFee(dep.getId(), recipeIdList, null));

        depDetailList.add(depDetailBean);
        LOG.info("CommonShowDepService获取到的药店列表:{}.", JSONUtils.toString(depDetailList));
    }
}
