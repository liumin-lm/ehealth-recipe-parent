package recipe.purchase;

import com.ngari.recipe.common.RecipeResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.RecipeBussConstant;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 在线支付-配送到家购药方式
 * @version： 1.0
 */
public class PayModeOnline implements IPurchaseService{
    /** logger */
    private static final Logger LOG = LoggerFactory.getLogger(PayModeOnline.class);

    @Override
    public RecipeResultBean findSupportDepList(Integer recipeId) {
        LOG.info("PayModeOnline findSupportDepList ..............");
        return null;
    }

    @Override
    public RecipeResultBean order(Integer recipeId) {
        return null;
    }

    @Override
    public Integer getPayMode() {
        return RecipeBussConstant.PAYMODE_ONLINE;
    }

    @Override
    public String getServiceName() {
        return "payModeOnlineService";
    }
}
