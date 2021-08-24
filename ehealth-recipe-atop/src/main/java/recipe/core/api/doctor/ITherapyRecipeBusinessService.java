package recipe.core.api.doctor;

import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.RecipeTherapy;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.doctor.RecipeTherapyVO;

import java.util.List;

public interface ITherapyRecipeBusinessService {
    /**
     * 保存诊疗处方
     *
     * @param recipeInfoVO
     * @return
     */
    Integer saveTherapyRecipe(RecipeInfoVO recipeInfoVO);

    /**
     * 获取诊疗处方列表
     *
     * @param recipeTherapy 诊疗处方对象
     * @param start         页数
     * @param limit         每页条数
     * @return
     */
    List<RecipeInfoDTO> therapyRecipeList(RecipeTherapy recipeTherapy, int start, int limit);

    /**
     * 获取诊疗处方明细
     *
     * @param recipeId 处方id
     * @return
     */
    RecipeInfoDTO therapyRecipeInfo(Integer recipeId);

    /**
     * 撤销诊疗处方
     *
     * @param recipeTherapyVO
     * @return
     */
    boolean cancelRecipe(RecipeTherapyVO recipeTherapyVO);

    /**
     * 作废诊疗处方
     * @param recipeId
     * @return
     */
    boolean abolishTherapyRecipe(Integer recipeId);

}
