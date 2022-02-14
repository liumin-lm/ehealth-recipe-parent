package recipe.manager;

import org.springframework.stereotype.Service;
import recipe.enumerate.status.RecipeStateEnum;

/**
 * 状态处理通用类：处方状态 ，订单状态，
 * @author fuzi
 */
@Service
public class StateManager {

    public Boolean updateRecipeState(Integer recipeId, RecipeStateEnum processState,RecipeStateEnum subState) {


        return true;
    }
}
