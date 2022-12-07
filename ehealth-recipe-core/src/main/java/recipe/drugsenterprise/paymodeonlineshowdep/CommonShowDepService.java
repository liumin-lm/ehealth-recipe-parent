package recipe.drugsenterprise.paymodeonlineshowdep;

import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.enumerate.type.StandardPaymentWayEnum;
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
        if (StandardPaymentWayEnum.PAYMENT_WAY_COD.getType().toString().equals(dep.getPayMode())) {
            depDetailBean.setPayModeText(StandardPaymentWayEnum.PAYMENT_WAY_COD.getName());
            depDetailBean.setPayMode(StandardPaymentWayEnum.PAYMENT_WAY_COD.getType());
        } else {
            depDetailBean.setPayModeText(StandardPaymentWayEnum.PAYMENT_WAY_ONLINE.getName());
            depDetailBean.setPayMode(StandardPaymentWayEnum.PAYMENT_WAY_ONLINE.getType());
        }

        depDetailBean.setPayModeText(StandardPaymentWayEnum.PAYMENT_WAY_ONLINE.getName());
        depDetailBean.setPayMode(StandardPaymentWayEnum.PAYMENT_WAY_ONLINE.getType());

        RecipeOrderService recipeOrderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        //重置药企处方价格
        depDetailBean.setRecipeFee(recipeOrderService.reCalculateRecipeFee(dep.getId(), recipeIdList, null));

        depDetailList.add(depDetailBean);
        LOG.info("CommonShowDepService获取到的药店列表:{}.", JSONUtils.toString(depDetailList));
    }
}
