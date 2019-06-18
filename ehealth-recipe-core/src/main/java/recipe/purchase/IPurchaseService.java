package recipe.purchase;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;

import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 购药方式行为
 * @version： 1.0
 */
public interface IPurchaseService {


    /**
     * 获取供应商列表
     *
     * @param dbRecipe
     * @param ext
     * @return
     */
    RecipeResultBean findSupportDepList(Recipe dbRecipe, Map ext);

    /**
     * 下单提交方法
     *
     * @param recipeId
     * @return
     */
    RecipeResultBean order(Integer recipeId);

    /**
     * RecipeBussConstant 中常量值，前端约定值
     *
     * @return
     */
    Integer getPayMode();

    /**
     * 需要在 PurchaseEnum 中添加该值及 PurchaseConfigure 增加该Service实例
     *
     * @return
     */
    String getServiceName();
}
