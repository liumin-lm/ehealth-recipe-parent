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

    /**
     * 撤销诊疗处方
     * @param recipeTherapyVO
     * @return
     */
    boolean cancelRecipe(RecipeTherapyVO recipeTherapyVO);

    /**
     * 作废诊疗处方
     * @param therapyId
     * @return
     */
    boolean abolishTherapyRecipe(Integer therapyId);
}
