package recipe.core.api.doctor;

import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.RecipeTherapy;
import com.ngari.recipe.vo.ItemListVO;
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
     * 获取诊疗处方总数
     *
     * @param recipeTherapy
     * @return
     */
    Integer therapyRecipeTotal(RecipeTherapy recipeTherapy);

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
     * 作废诊疗处方
     * @param recipeId 处方ID
     * @return 作废结果
     */
    boolean abolishTherapyRecipe(Integer recipeId);

    /**
     * 复诊关闭作废诊疗处方
     *
     * @param recipeTherapyVO 诊疗处方
     * @return 作废结果
     */
    boolean abolishTherapyRecipeForRevisitClose(RecipeTherapyVO recipeTherapyVO);

    /**
     * 更新诊疗处方
     * @param recipeTherapyVO 诊疗处方
     * @return void
     */
    void updateTherapyRecipe(RecipeTherapyVO recipeTherapyVO);

    /**
     * 搜索诊疗项目
     * @param itemListVO 诊疗项目
     * @return 诊疗项目列表
     */
    List<ItemListVO> searchItemListByKeyWord(ItemListVO itemListVO);


}
