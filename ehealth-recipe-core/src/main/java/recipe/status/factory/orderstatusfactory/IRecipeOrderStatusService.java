package recipe.status.factory.orderstatusfactory;

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
     * 更新状态
     *
     * @param orderStatus
     * @return
     */
    Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder);
}
