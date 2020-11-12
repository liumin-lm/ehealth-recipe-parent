package recipe.atop;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.service.IRecipeOrderTwoService;
import com.ngari.recipe.vo.ResultBean;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeStatusConstant;
import recipe.service.RecipeMsgService;

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
    public ResultBean updateRecipeOrderStatus(UpdateOrderStatusVO updateOrderStatusVO) {
        logger.info("RecipeOrderAtop updateRecipeOrderStatus updateOrderStatusVO = {}", JSON.toJSONString(updateOrderStatusVO));
        try {
            ResultBean result = recipeOrderTwoService.updateRecipeOrderStatus(updateOrderStatusVO);
            this.sendExpressMsg(updateOrderStatusVO);
            logger.info("RecipeOrderAtop updateRecipeOrderStatus result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeOrderAtop updateRecipeOrderStatus error", e);
            return ResultBean.serviceError(e.getMessage(), false);

        }
    }

    /**
     * 更新快递信息后，发送消息
     * @param orderStatus
     */
    public void sendExpressMsg(UpdateOrderStatusVO orderStatus) {
        if (!ObjectUtils.isEmpty(orderStatus.getLogisticsCompany())
                || StringUtils.isNotBlank(orderStatus.getTrackingNumber())) {
            RecipeMsgService.batchSendMsg(orderStatus.getRecipeId(), RecipeStatusConstant.EXPRESSINFO_REMIND);
        }
    }
}
