package recipe.atop;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.service.IRecipeOrderTwoService;
import com.ngari.recipe.vo.CodeEnum;
import com.ngari.recipe.vo.ResultBean;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeMsgEnum;
import recipe.drugsenterprise.ThirdEnterpriseCallService;
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
            if(result.getCode() == CodeEnum.SERVICE_SUCCEED.getCode()) {
                // 同步运单信息至基础服务
                ThirdEnterpriseCallService.sendLogisticsInfoToBase(updateOrderStatusVO.getRecipeId(),updateOrderStatusVO.getLogisticsCompany()+"",updateOrderStatusVO.getTrackingNumber());
                this.sendExpressMsg(updateOrderStatusVO);
            }
            logger.info("RecipeOrderAtop updateRecipeOrderStatus result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeOrderAtop updateRecipeOrderStatus error", e1);
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
            RecipeMsgService.batchSendMsg(orderStatus.getRecipeId(), RecipeMsgEnum.EXPRESSINFO_REMIND.getStatus());
        }
    }
}
