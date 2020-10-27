package recipe.status.factory.recipestatusfactory.impl;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.stereotype.Service;
import recipe.status.factory.constant.RecipeOrderStatusEnum;

/**
 * 已拒发
 *
 * @author fuzi
 */
@Service
public class StatusDeclineImpl extends AbstractRecipeOrderStatus {
    @Override
    public Integer getStatus() {
        return RecipeOrderStatusEnum.ORDER_STATUS_DECLINE.getType();
    }

    @Override
    public Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder) {
        return null;
    }
}
