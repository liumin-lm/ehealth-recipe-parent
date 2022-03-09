package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.exception.DAOException;
import eh.base.constant.ErrorCode;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.enumerate.status.OrderStateEnum;
import recipe.enumerate.status.RecipeAuditStateEnum;
import recipe.enumerate.status.RecipeStateEnum;

/**
 * 状态处理通用类：处方状态 ，订单状态，审方状态
 *
 * @author fuzi
 */
@Service
public class StateManager extends BaseManager {

    /**
     * 修改订单状态
     *
     * @param orderId     订单id
     * @param processState 父状态枚举
     * @param subState     子状态枚举
     * @return
     */
    @LogRecord
    public Boolean updateOrderState(Integer orderId, OrderStateEnum processState, OrderStateEnum subState) {
        RecipeOrder recipeOrder = recipeOrderDAO.get(orderId);
        logger.info("StateManager updateOrderState recipeOrder:{}", JSON.toJSONString(recipeOrder));
        if (null == recipeOrder) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该订单不存在");
        }
        boolean result;
        switch (processState) {
            case PROCESS_STATE_CANCELLATION:
                result = this.cancelOrder(recipeOrder, processState, subState);
                break;
            default:
                result = false;
                break;
        }
        return result;
    }

    /**
     * 修改处方状态
     *
     * @param recipeId     处方id
     * @param processState 父状态枚举
     * @param subState     子状态枚举
     * @return
     */
    @LogRecord
    public Boolean updateRecipeState(Integer recipeId, RecipeStateEnum processState, RecipeStateEnum subState) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        logger.info("StateManager updateRecipeState recipe:{}", JSON.toJSONString(recipe));
        if (null == recipe) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方不存在");
        }
        boolean result;
        switch (processState) {
            case PROCESS_STATE_DELETED:
            case PROCESS_STATE_CANCELLATION:
                result = this.cancellation(recipe, processState, subState);
                break;
            default:
                result = false;
                break;
        }
        saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), subState.getName());
        return result;
    }

    @LogRecord
    public Boolean updateAuditState(Integer recipeId, RecipeAuditStateEnum state) {
        Recipe updateRecipe = new Recipe();
        updateRecipe.setRecipeId(recipeId);
        updateRecipe.setAuditState(state.getType());
        recipeDAO.updateNonNullFieldByPrimaryKey(updateRecipe);
        return true;
    }

    /**
     * 作废订单
     *
     * @param order        订单信息
     * @param processState 父状态枚举
     * @param subState     子状态枚举
     * @return
     */
    private Boolean cancelOrder(RecipeOrder order, OrderStateEnum processState, OrderStateEnum subState) {
        RecipeOrder updateOrder = new RecipeOrder(order.getOrderId(),processState.getType(),subState.getType());
        logger.info("updateOrder:{}",JSONArray.toJSONString(updateOrder));
        updateOrder.setOrderId(order.getOrderId());
        updateOrder.setProcessState(processState.getType());
        updateOrder.setSubState(subState.getType());
        recipeOrderDAO.updateNonNullFieldByPrimaryKey(updateOrder);
        return true;
    }

    /**
     * 作废处方
     *
     * @param recipe       处方信息
     * @param processState 父状态枚举
     * @param subState     子状态枚举
     * @return
     */
    private Boolean cancellation(Recipe recipe, RecipeStateEnum processState, RecipeStateEnum subState) {
        if (RecipeStateEnum.PROCESS_STATE_DELETED == processState && recipe.getProcessState() > 1) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方单不是暂存处方不能删除");
        }
        if (RecipeStateEnum.PROCESS_STATE_DELETED.getType().equals(recipe.getProcessState())
                || RecipeStateEnum.PROCESS_STATE_CANCELLATION.getType().equals(recipe.getProcessState())) {
            return true;
        }

        Recipe updateRecipe = new Recipe();
        //  1. 医生撤销 2.订单过去未操作 3.超时未支付 4.药商拒绝
        if ((RecipeStateEnum.SUB_CANCELLATION_DOCTOR == subState ||
             RecipeStateEnum.SUB_CANCELLATION_REFUSE_ORDER == subState ||
             RecipeStateEnum.SUB_CANCELLATION_TIMEOUT_NOT_MEDICINE == subState ||
             RecipeStateEnum.SUB_CANCELLATION_TIMEOUT_NOT_ORDER == subState )
                && recipe.getAuditState().equals(RecipeAuditStateEnum.PENDING_REVIEW.getType())) {
            updateRecipe.setAuditState(RecipeAuditStateEnum.NO_REVIEW.getType());
        }

        updateRecipe.setRecipeId(recipe.getRecipeId());
        updateRecipe.setProcessState(processState.getType());
        updateRecipe.setSubState(subState.getType());
        recipeDAO.updateNonNullFieldByPrimaryKey(updateRecipe);
        return true;
    }
}
