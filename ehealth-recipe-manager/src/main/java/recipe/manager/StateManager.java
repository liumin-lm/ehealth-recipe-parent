package recipe.manager;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.exception.DAOException;
import eh.base.constant.ErrorCode;
import org.springframework.stereotype.Service;
import recipe.enumerate.status.OrderStateEnum;
import recipe.enumerate.status.RecipeStateEnum;

import static recipe.enumerate.status.RecipeStateEnum.SUB_CANCELLATION_DOCTOR;

/**
 * 状态处理通用类：处方状态 ，订单状态，
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
    public Boolean updateRecipeState(Integer recipeId, RecipeStateEnum processState, RecipeStateEnum subState) {
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
        return result;
    }

    /**
     * 作废订单
     *
     * @param order       订单信息
     * @param processState 父状态枚举
     * @param subState     子状态枚举
     * @return
     */
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
    private Boolean cancellation(Recipe recipe, RecipeStateEnum processState, RecipeStateEnum subState) {
        if (null != recipe.getProcessState() && recipe.getProcessState() > 1) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方单不是暂存处方不能删除");
        }
        // todo 医生撤销情况下 待审核审方 改为无需审核状态
        if (SUB_CANCELLATION_DOCTOR == subState) {
            
        }
        Recipe updateRecipe = new Recipe();
        updateRecipe.setRecipeId(recipe.getRecipeId());
        updateRecipe.setProcessState(processState.getType());
        updateRecipe.setSubState(subState.getType());
        recipeDAO.updateNonNullFieldByPrimaryKey(updateRecipe);
        return true;
    }
}
