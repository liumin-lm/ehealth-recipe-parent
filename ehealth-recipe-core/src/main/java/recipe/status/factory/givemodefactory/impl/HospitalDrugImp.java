package recipe.status.factory.givemodefactory.impl;

import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.springframework.stereotype.Service;
import recipe.constant.RecipeStatusConstant;
import recipe.status.factory.constant.GiveModeEnum;

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
        orderStatus.setTargetRecipeStatus(RecipeStatusConstant.CHECK_PASS);
        recipeOrderStatusProxy.updateOrderByStatus(orderStatus);
    }
}
