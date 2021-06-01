package recipe.factory.status.givemodefactory.impl;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.stereotype.Service;
import recipe.factory.status.constant.GiveModeEnum;

/**
 * 医院取药
 *
 * @author fuzi
 */
@Service
public class HospitalDrugImp extends AbstractGiveMode {
    @Override
    public Integer getGiveMode() {
        return GiveModeEnum.GIVE_MODE_HOSPITAL_DRUG.getType();
    }

    @Override
    public void updateStatus(UpdateOrderStatusVO orderStatus) {
        orderStatus.setSender("");
        RecipeOrder recipeOrder = new RecipeOrder(orderStatus.getOrderId());
        Recipe recipe = super.getRecipe(orderStatus.getRecipeId());
        recipeOrderStatusProxy.updateOrderByStatus(orderStatus, recipeOrder, recipe);
    }
}
