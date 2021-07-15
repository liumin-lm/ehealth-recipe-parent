package recipe.core.api.doctor;

import com.ngari.recipe.commonrecipe.model.CommonDTO;

import java.util.List;

/**
 * 常用方服务
 *
 * @author fuzi
 */
public interface ICommonRecipeService {
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
    List<CommonDTO> commonRecipeList(Integer organId, Integer doctorId, List<Integer> recipeType, int start, int limit);

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
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @return 线下常用方数据集合
     */
    List<CommonDTO> offlineCommon(Integer organId, Integer doctorId);

    /**
     * 添加线下常用方到线上
     *
     * @param commonList 线下常用方数据集合
     * @return boolean
     */
    List<String> addOfflineCommon(Integer organId, List<CommonDTO> commonList);
}
