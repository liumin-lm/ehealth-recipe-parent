package recipe.core.api;

import recipe.vo.second.RevisitRecipeTraceVo;

import java.util.List;

/**
 * @author yinsheng
 * @date 2021\7\16 0016 17:16
 */
public interface IRevisitTraceBusinessService {

    /**
     * 复诊处方追溯详情
     *
     * @param bussSource 处方来源
     * @param clinicId   业务id
     * @return
     */
    List<RevisitRecipeTraceVo> revisitRecipeTrace(Integer bussSource, Integer clinicId);

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
