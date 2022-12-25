package recipe.atop.job;

import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.patient.IRecipeOrderBusinessService;

/**
 * @description： 订单定时任务
 * @author： whf
 * @date： 2022-11-02 14:19
 */
@RpcBean(value = "recipeOrderJobAtop")
public class RecipeOrderJobAtop extends BaseAtop {

    @Autowired
    private IRecipeOrderBusinessService recipeOrderService;

    /**
     * 配送中订单 完成 定时任务
     */
    @RpcService
    public void finishRecipeOrderJob() {
        recipeOrderService.finishRecipeOrderJob();
    }

    /**
     * 获取his结算信息 诸暨专用
     */
    @RpcService
    public void findHisSettle() {
        recipeOrderService.findHisSettle();
    }
}
