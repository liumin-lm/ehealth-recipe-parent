package recipe.core.api.doctor;

import com.ngari.recipe.recipe.model.CancelRecipeReqVO;
import com.ngari.recipe.recipe.model.CancelRecipeResultVO;
import recipe.vo.doctor.RecipeInfoVO;

public interface ITherapyRecipeBusinessService {
    RecipeInfoVO saveTherapyRecipe(RecipeInfoVO recipeInfoVO);

    CancelRecipeResultVO cancelRecipe(CancelRecipeReqVO cancelRecipeReqVO);

    void abolishTherapyRecipe(Integer therapyId);
}
