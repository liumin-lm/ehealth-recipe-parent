package recipe.core.api.doctor;

import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.doctor.RecipeTherapyVO;

public interface ITherapyRecipeBusinessService {
    /**
     * 保存诊疗处方
     *
     * @param recipeInfoVO
     * @return
     */
    Integer saveTherapyRecipe(RecipeInfoVO recipeInfoVO);

    RecipeTherapyVO cancelRecipe(RecipeTherapyVO recipeTherapyVO);

    boolean abolishTherapyRecipe(Integer therapyId);
}
