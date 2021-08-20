package recipe.manager;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import org.springframework.stereotype.Service;
import recipe.util.ValidateUtil;

import java.util.List;

/**
 * 处方明细
 *
 * @author fuzi
 */
@Service
public class RecipeDetailManager extends BaseManager {

    public List<Recipedetail> saveRecipeDetails(List<Recipedetail> details, Recipe recipe) {
        recipeDetailDAO.updateDetailInvalidByRecipeId(recipe.getRecipeId());
        for (Recipedetail detail : details) {
            detail.setRecipeId(recipe.getRecipeId());
            detail.setStatus(1);
            if (ValidateUtil.integerIsEmpty(detail.getRecipeDetailId())) {
                recipeDetailDAO.save(detail);
            } else {
                recipeDetailDAO.update(detail);
            }
        }
        return details;
    }
}
