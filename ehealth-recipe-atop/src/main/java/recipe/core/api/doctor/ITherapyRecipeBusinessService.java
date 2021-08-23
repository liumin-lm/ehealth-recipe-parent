package recipe.core.api.doctor;

import com.ngari.recipe.dto.RecipeInfoDTO;
import recipe.vo.doctor.ItemListVO;
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

    /**
     * 搜索诊疗项目
     * @param itemListVO
     * @return
     */
    List<ItemListVO> searchItemListByKeyWord(ItemListVO itemListVO);

    /**
     * 删除诊疗项目
     * @param id
     */
    void deleteItemListById(Integer id);

    /**
     * 更新诊疗状态
     * @param id
     * @param status
     */
    void updateStatusById(Integer id, Integer status);

}
