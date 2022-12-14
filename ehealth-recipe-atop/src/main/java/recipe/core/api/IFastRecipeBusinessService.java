package recipe.core.api;

import com.ngari.recipe.vo.FastRecipeReq;
import com.ngari.recipe.entity.FastRecipe;
import com.ngari.recipe.entity.FastRecipeDetail;
import com.ngari.recipe.vo.FastRecipeVO;
import ctd.util.annotation.RpcService;
import recipe.vo.doctor.RecipeInfoVO;
import java.util.List;
import java.util.Map;

/**
 * 便捷购药
 */
public interface IFastRecipeBusinessService {
    /**
     * 便捷购药，患者端调用开处方单
     *
     * @param recipeInfoVOList
     * @return
     */
    List<Integer> fastRecipeSaveRecipeList(List<RecipeInfoVO> recipeInfoVOList);


    /**
     * 便捷购药 运营平台新增药方
     *
     * @param recipeId,title
     * @return
     */
    Integer addFastRecipe(Integer recipeId, String title);

    /**
     * 查询药方列表
     *
     * @param fastRecipeReq
     * @return
     */
    List<FastRecipe> findFastRecipeListByParam(FastRecipeReq fastRecipeReq);

    /**
     * 根据药方id查询药方列表
     *
     * @param id
     * @return
     */
    @RpcService
    List<FastRecipeDetail> findFastRecipeDetailsByFastRecipeId(Integer id);

    /**
     * 运营平台药方列表页更新
     *
     * @param fastRecipeVO
     * @return
     */
    Boolean simpleUpdateFastRecipe(FastRecipeVO fastRecipeVO);

    /**
     * 运营平台药方详情页更新
     *
     * @param fastRecipeVO
     * @return
     */
    Boolean fullUpdateFastRecipe(FastRecipeVO fastRecipeVO);

    /**
     * 便捷购药运营平台查询处方单
     *
     * @param recipeId
     * @param organId
     * @return
     */
    Map<String, Object> findRecipeAndDetailsByRecipeIdAndOrgan(Integer recipeId, Integer organId);
}
