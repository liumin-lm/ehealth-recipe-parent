package recipe.api.open;

import ctd.util.annotation.RpcService;
import recipe.vo.second.RevisitRecipeTraceVo;

import java.util.List;

/**
 * 处方提供的服务接口
 *
 * @author zhaoh
 * @date 2021/7/19
 */
public interface IRecipeAtopService {
    /**
     * 查询是否存在药师未审核状态的处方
     *
     * @param bussSource 处方来源
     * @param clinicID   复诊ID
     * @return True存在 False不存在
     * @date 2021/7/19
     */
    @RpcService
    Boolean existUncheckRecipe(Integer bussSource, Integer clinicID);

    /**
     * 复诊处方追溯
     *
     * @param bussSource 处方来源
     * @param clinicID   复诊ID
     * @return
     */
    @RpcService
    List<RevisitRecipeTraceVo> revisitRecipeTrace(Integer bussSource, Integer clinicID);

    /**
     * 复诊处方追溯列表数据处理
     *
     * @param startTime
     * @param endTime
     * @param recipeIds
     * @param organId
     */
    @RpcService
    void handDealRevisitTraceRecipe(String startTime, String endTime, List<Integer> recipeIds, Integer organId);

}
