package recipe.drugsenterprise.paymodeonlineshowdep;

import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.RecipeBussConstant;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.service.RecipeOrderService;

import java.util.Arrays;
import java.util.List;

/**
 * created by shiyuping on 2020/11/10
 * @author shiyuping
 * 展示药店标识的药企
 */
public class ShowStoreFlagDepService implements PayModeOnlineShowDepInterface {
    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ShowStoreFlagDepService.class);

    @Override
    public void getPayModeOnlineShowDep(DrugsEnterprise dep, List<DepDetailBean> depDetailList, Recipe dbRecipe, List<Integer> recipeIdList) {
        //特殊处理,对华润药企特殊处理,包含华润药企,需要将华润药企替换成药店
        RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        //需要从接口获取药店列表
        DrugEnterpriseResult drugEnterpriseResult = remoteDrugService.findSupportDep(Arrays.asList(dbRecipe.getRecipeId()), null, dep);
        if (DrugEnterpriseResult.SUCCESS.equals(drugEnterpriseResult.getCode())) {
            Object result = drugEnterpriseResult.getObject();
            if (result != null && result instanceof List) {
                List<DepDetailBean> hrList = (List) result;
                for (DepDetailBean depDetailBean : hrList) {
                    depDetailBean.setDepId(dep.getId());
                    depDetailBean.setBelongDepName(depDetailBean.getDepName());
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
                }
                depDetailList.addAll(hrList);
            }
        }
        LOG.info("ShowStoreFlagDepService获取到的药店列表:{}.", JSONUtils.toString(depDetailList));
    }
}
