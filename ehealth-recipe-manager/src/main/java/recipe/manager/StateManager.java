package recipe.manager;

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
        logger.info("StateManager updateRecipeState recipeId ={},processState={},subState={} ", recipeId, processState, subState);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
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
        logger.info("StateManager updateRecipeState recipeId ={} ", recipeId);
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
    @LogRecord
    private Boolean cancelOrder(RecipeOrder order, OrderStateEnum processState, OrderStateEnum subState) {
        RecipeOrder updateOrder = new RecipeOrder(order.getOrderId(),processState.getType(),subState.getType());
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
    @LogRecord
    private Boolean cancellation(Recipe recipe, RecipeStateEnum processState, RecipeStateEnum subState) {
        if (RecipeStateEnum.PROCESS_STATE_DELETED == processState && recipe.getProcessState() > 1) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方单不是暂存处方不能删除");
        }
        Recipe updateRecipe = new Recipe();
        //  医生撤销情况下 待审核审方 改为无需审核状态
        if (RecipeStateEnum.SUB_CANCELLATION_DOCTOR == subState && recipe.getAuditState() < RecipeAuditStateEnum.PENDING_REVIEW.getType()) {
            updateRecipe.setAuditState(RecipeAuditStateEnum.NO_REVIEW.getType());
        }

        updateRecipe.setRecipeId(recipe.getRecipeId());
        updateRecipe.setProcessState(processState.getType());
        updateRecipe.setSubState(subState.getType());
        recipeDAO.updateNonNullFieldByPrimaryKey(updateRecipe);
        return true;
    }
}
