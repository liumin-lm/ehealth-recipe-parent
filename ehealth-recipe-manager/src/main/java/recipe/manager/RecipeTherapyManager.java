package recipe.manager;

import com.ngari.recipe.entity.RecipeTherapy;
import org.springframework.stereotype.Service;

/**
 * 诊疗处方
 *
 * @author fuzi
 */
@Service
public class RecipeTherapyManager extends BaseManager {
    public RecipeTherapy saveRecipeTherapy(RecipeTherapy recipeTherapy) {
        return recipeTherapy;
    }
}
