package recipe.bean;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @author fuzi
 */
@Setter
@Getter
public class RecipeDTO implements Serializable {
    private static final long serialVersionUID = -9005230183318990440L;
    private Recipe recipe;
    private RecipeExtend recipeExtend;
    private List<Recipedetail> recipeDetails;
    private RecipeOrder recipeOrder;
}
