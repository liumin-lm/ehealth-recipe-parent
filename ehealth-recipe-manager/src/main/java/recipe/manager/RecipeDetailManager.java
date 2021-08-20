package recipe.manager;

import com.ngari.recipe.entity.Recipedetail;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 处方明细
 *
 * @author fuzi
 */
@Service
public class RecipeDetailManager extends BaseManager {
    public List<Recipedetail> saveRecipeDetails(List<Recipedetail> details) {
        return details;
    }
}
