package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.vo.ResultBean;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeOrderService;
import recipe.util.ValidateUtil;

/**
 * 处方订单服务入口类
 *
 * @author fuzi
 */
@RpcBean("recipeOrderAtop")
public class RecipeOrderDoctorAtop extends BaseAtop {

    @Autowired
    private IRecipeOrderService recipeOrderTwoService;

    /**
     * 订单状态更新
     */
    @RpcService
    public ResultBean updateRecipeOrderStatus(UpdateOrderStatusVO updateOrderStatusVO) {
        logger.info("RecipeOrderAtop updateRecipeOrderStatus updateOrderStatusVO = {}", JSON.toJSONString(updateOrderStatusVO));
        if (ValidateUtil.integerIsEmpty(updateOrderStatusVO.getRecipeId()) || null == updateOrderStatusVO.getTargetRecipeOrderStatus()) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参为空");
        }
        try {
            ResultBean result = recipeOrderTwoService.updateRecipeOrderStatus(updateOrderStatusVO);
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
     * 更新核发药师信息
     *
     * @param recipeId
     * @param giveUser
     * @return
     */
    @RpcService
    public ResultBean updateRecipeGiveUser(Integer recipeId, Integer giveUser) {
        logger.info("RecipeOrderAtop updateRecipeGiveUser recipeId = {} giveUser = {}", recipeId, giveUser);
        validateAtop(recipeId, giveUser);
        try {
            ResultBean result = recipeOrderTwoService.updateRecipeGiveUser(recipeId, giveUser);
            logger.info("RecipeOrderAtop updateRecipeGiveUser result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeOrderAtop updateRecipeGiveUser error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeOrderAtop updateRecipeGiveUser error", e);
            return ResultBean.serviceError(e.getMessage(), false);
        }
    }
}
