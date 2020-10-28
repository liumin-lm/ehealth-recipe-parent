package recipe.atop;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.service.IRecipeOrderTwoService;
import com.ngari.recipe.vo.ResultBean;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 处方订单服务入口类
 *
 * @author fuzi
 */
@RpcBean("recipeOrderAtop")
public class RecipeOrderAtop extends BaseAtop {

    @Autowired
    private IRecipeOrderTwoService recipeOrderTwoService;

    /**
     * 订单状态更新
     */
    @RpcService
    public ResultBean<Boolean> updateRecipeOrderStatus(UpdateOrderStatusVO updateOrderStatusVO) {
        logger.info("RecipeOrderAtop updateRecipeOrderStatus updateOrderStatusVO = {}", JSON.toJSONString(updateOrderStatusVO));
        try {
            ResultBean<Boolean> result = recipeOrderTwoService.updateRecipeOrderStatus(updateOrderStatusVO);
            logger.info("RecipeOrderAtop updateRecipeOrderStatus result = {}", JSON.toJSONString(result));
            return result;
        } catch (Exception e) {
            logger.error("RecipeOrderAtop updateRecipeOrderStatus error", e);
            return new ResultBean<>(609, e.getMessage(), false);
        }
    }
}
