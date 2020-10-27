package recipe.status.factory.recipestatusfactory.impl;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.stereotype.Service;
import recipe.status.factory.constant.RecipeOrderStatusEnum;

/**
 * 已发药
 *
 * @author fuzi
 */
@Service
public class StatusDoneDispensingImpl extends AbstractRecipeOrderStatus {
    @Override
    public Integer getStatus() {
        return RecipeOrderStatusEnum.ORDER_STATUS_DONE_DISPENSING.getType();
    }

    @Override
    public Recipe updateStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder) {
        return null;
    }
}
