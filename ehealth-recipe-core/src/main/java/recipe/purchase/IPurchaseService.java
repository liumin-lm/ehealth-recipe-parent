package recipe.purchase;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;

import java.util.List;
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
     * @param extInfo
     * @return
     */
    RecipeResultBean findSupportDepList(Recipe dbRecipe, Map<String, String> extInfo);

    /**
     * 下单提交方法
     *
     * @param dbRecipes
     * @param extInfo
     * @return
     */
    OrderCreateResult order(List<Recipe> dbRecipes, Map<String, String> extInfo);

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

    /**
     * 获取提示文案
     * @param recipe
     * @param order
     * @return
     */
    String getTipsByStatusForPatient(Recipe recipe, RecipeOrder order);

    /**
     * 获取订单的状态
     * @param recipe 处方详情
     * @return 订单状态
     */
    Integer getOrderStatus(Recipe recipe);

    /**
     * 设置处方药品费用的支付方式 0 线上支付 1 线下支付
     * @param recipeOrder 订单信息
     */
    void setRecipePayWay(RecipeOrder recipeOrder);
}
