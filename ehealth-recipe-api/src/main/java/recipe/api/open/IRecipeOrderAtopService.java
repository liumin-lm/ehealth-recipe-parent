package recipe.api.open;

import com.ngari.common.dto.CheckRequestCommonOrderPageDTO;
import com.ngari.common.dto.SyncOrderVO;
import ctd.util.annotation.RpcService;
import recipe.vo.second.RecipeOrderVO;

/**
 * @description： 处方订单第三方
 * @author： whf
 * @date： 2021-11-08 15:35
 */
public interface IRecipeOrderAtopService {

    /**
     * 根据订单id 查询订单信息
     * @param orderId
     * @return
     */
    @RpcService
    RecipeOrderVO getRecipeOrderByBusId(Integer orderId);

    /**
     *  端历史数据同步使用
     * @param request
     * @return
     */
    @RpcService
    CheckRequestCommonOrderPageDTO getRecipePageForCommonOrder(SyncOrderVO request);
}
