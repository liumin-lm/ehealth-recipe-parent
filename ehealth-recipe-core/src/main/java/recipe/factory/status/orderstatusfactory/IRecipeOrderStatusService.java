package recipe.factory.status.orderstatusfactory;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;

/**
 * @author fuzi
 */
public interface IRecipeOrderStatusService {
    /**
     * 获取实现类 类型
     *
     * @return
     */
    Integer getStatus();

    /**
     * 根据订单状态 更新处方状态
     *
     * @param orderStatus 入参
     * @param recipeOrder 变更订单数据
     * @return
     */
    Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder, Recipe recipe);

    /**
     * 异步处方信息上传
     *
     * @param recipe
     */
    void upRecipeThreadPool(Recipe recipe);

    /**
     * 合并支付 更新同组处方 状态
     * @param recipe
     * @param orderId
     */
    void updateGroupRecipe(Recipe recipe,Integer orderId);
}
