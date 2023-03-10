package recipe.core.api.greenroom;

import com.ngari.recipe.entity.RecipeRefund;
import recipe.vo.greenroom.*;

import java.util.List;

/**
 * 退费查询接口调用
 *
 * @author ys
 */
public interface IRecipeOrderRefundService {

    /**
     * 查询药品订单列表
     * @param recipeOrderRefundReqVO
     * @return
     */
    RecipeOrderRefundPageVO findRefundRecipeOrder(RecipeOrderRefundReqVO recipeOrderRefundReqVO);

    /**
     * 查询药品订单详情
     * @param orderCode
     * @param busType
     * @return
     */
    RecipeOrderRefundDetailVO getRefundOrderDetail(String orderCode, Integer busType);

    /**
     * 强制退费
     * @param auditRefundVO
     */
    void forceRefund(AuditRefundVO auditRefundVO);

    /**
     * 查询患者申请退费记录
     * @param recipeId
     * @return
     */
    RecipeRefund findApplyRefund(Integer recipeId);

    /**
     * 重置处方药企推送状态
     * @param recipeIds
     * */
    void updateRecipePushFlag(List<Integer> recipeIds);

    /**
     * 根据订单id 查询 订单编号
     * @param orderId
     * @return
     */
    String getOrderCodeByOrderId(Integer orderId);
}
