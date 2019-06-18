package recipe.purchase;

import com.ngari.recipe.common.RecipeResultBean;
import recipe.constant.RecipeBussConstant;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 药店取药购药方式
 * @version： 1.0
 */
public class PayModeTFDS implements IPurchaseService{

    @Override
    public RecipeResultBean findSupportDepList(Integer recipeId) {
        return null;
    }

    @Override
    public RecipeResultBean order(Integer recipeId) {
        return null;
    }

    @Override
    public Integer getPayMode() {
        return RecipeBussConstant.PAYMODE_TFDS;
    }

    @Override
    public String getServiceName() {
        return "payModeTFDSService";
    }
}
