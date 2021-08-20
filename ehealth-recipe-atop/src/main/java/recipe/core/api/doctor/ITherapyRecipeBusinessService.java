package recipe.core.api.doctor;

import recipe.vo.doctor.RecipeInfoVO;

public interface ITherapyRecipeBusinessService {
    /**
     * 保存诊疗处方
     *
     * @param recipeInfoVO
     * @return
     */
    Integer saveTherapyRecipe(RecipeInfoVO recipeInfoVO);
}
