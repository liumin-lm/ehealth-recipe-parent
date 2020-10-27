package recipe.atop;

import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;

/**
 * 处方订单服务入口类
 *
 * @author fuzi
 */
@RpcBean("orderAtop")
public class OrderAtop extends BaseAtop {

    /**
     * 订单状态更新
     */
    @RpcService
    public void updateRecipeOrderStatus() {

    }
}
