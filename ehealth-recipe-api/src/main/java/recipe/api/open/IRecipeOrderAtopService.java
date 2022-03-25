package recipe.api.open;

import com.ngari.common.dto.CheckRequestCommonOrderPageDTO;
import com.ngari.common.dto.SyncOrderVO;
import ctd.util.annotation.RpcService;
import recipe.vo.second.RecipeOrderVO;
import recipe.vo.second.enterpriseOrder.DownOrderRequestVO;
import recipe.vo.second.enterpriseOrder.EnterpriseDownDataVO;

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
    @RpcService(mvcDisabled = true)
    RecipeOrderVO getRecipeOrderByBusId(Integer orderId);

    /**
     *  端历史数据同步使用
     * @param request
     * @return
     */
    @RpcService(mvcDisabled = true)
    CheckRequestCommonOrderPageDTO getRecipePageForCommonOrder(SyncOrderVO request);

    /**
     *  根据订单号更新物流单号
     * @param orderCode 订单号
     * @param trackingNumber 物流单号
     * @return 是否更新成功
     */
    @RpcService(mvcDisabled = true)
    Boolean updateTrackingNumberByOrderCode(String orderCode, String trackingNumber);

    /**
     * 第三方查询平台处方订单信息
     * @param downOrderRequestVO 请求入参
     * @return 处方订单列表
     */
    @RpcService
    EnterpriseDownDataVO findOrderAndRecipes(DownOrderRequestVO downOrderRequestVO);
}
