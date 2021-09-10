package recipe.core.api;

import recipe.vo.second.RevisitRecipeTraceVo;

import java.util.List;

/**
 * 处方复诊处理接口
 *
 * @author liumin
 * @date 2021\7\16 0016 17:16
 */
public interface IRevisitBusinessService {

    /**
     * 复诊处方追溯详情
     *
     * @param recipeId 处方ID
     * @param clinicId 复诊ID
     * @return
     */
    List<RevisitRecipeTraceVo> revisitRecipeTrace(Integer recipeId, Integer clinicId);

    /**
     * 复诊处方追溯列表数据处理
     *
     * @param startTime
     * @param endTime
     * @param recipeIds
     * @param organId
     */
    void handDealRevisitTraceRecipe(String startTime, String endTime, List<Integer> recipeIds, Integer organId);

}
