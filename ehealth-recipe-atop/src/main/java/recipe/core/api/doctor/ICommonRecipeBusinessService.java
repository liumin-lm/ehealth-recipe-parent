package recipe.core.api.doctor;

import com.ngari.recipe.commonrecipe.model.CommonDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDTO;
import com.ngari.recipe.dto.HisRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeBean;

import java.util.List;

/**
 * 常用方服务
 *
 * @author fuzi
 */
public interface ICommonRecipeBusinessService {
    /**
     * 获取常用方列表
     *
     * @param recipeType 处方类型
     * @param doctorId   医生id
     * @param organId    机构id
     * @param start      开始
     * @param limit      分页条数
     * @return
     */
    @Deprecated
    List<CommonDTO> commonRecipeList(Integer organId, Integer doctorId, List<Integer> recipeType, int start, int limit);

    /**
     * 获取常用方详情
     *
     * @param commonRecipeId 常用方id
     * @return
     */
    CommonDTO commonRecipeInfo(Integer commonRecipeId);

    /**
     * 新增或更新常用方
     *
     * @param common 常用方集合对象
     */
    void saveCommonRecipe(CommonDTO common);

    /**
     * 删除常用方
     *
     * @param commonRecipeId
     */
    void deleteCommonRecipe(Integer commonRecipeId);

    /**
     * 查询线下常用方
     * 产品规划功能废弃
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @return 线下常用方数据集合
     */
    @Deprecated
    List<CommonDTO> offlineCommon(Integer organId, Integer doctorId);

    /**
     * 添加线下常用方到线上
     *产品规划功能废弃
     * @param commonList 线下常用方数据集合
     * @return boolean
     */
    @Deprecated
    List<String> addOfflineCommon(Integer organId, List<CommonDTO> commonList);

    /**
     * 获取线下常用方列表
     *
     * @param recipeBean 查询入参
     * @return
     */
    List<CommonRecipeDTO> offlineCommonList(RecipeBean recipeBean);

    /**
     * 获取线下常用方详情
     *
     * @param commonRecipeCode 常用方编码-医院唯一主键字段
     * @return
     */
    HisRecipeDTO offlineCommonV1(Integer organId, String commonRecipeCode);


}
