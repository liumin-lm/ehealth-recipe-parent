package recipe.core.api.doctor;

import com.ngari.recipe.recipe.model.CancelRecipeReqVO;
import com.ngari.recipe.recipe.model.CancelRecipeResultVO;
import recipe.vo.doctor.RecipeInfoVO;

public interface ITherapyRecipeBusinessService {
    /**
     * 保存诊疗处方
     *
     * @param recipeInfoVO
     * @return
     */
    Integer saveTherapyRecipe(RecipeInfoVO recipeInfoVO);

    CancelRecipeResultVO cancelRecipe(CancelRecipeReqVO cancelRecipeReqVO);

    boolean abolishTherapyRecipe(Integer therapyId);
}
