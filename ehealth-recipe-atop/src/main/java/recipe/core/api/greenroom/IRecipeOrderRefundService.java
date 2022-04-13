package recipe.core.api.greenroom;

import recipe.vo.greenroom.AuditRefundVO;
import recipe.vo.greenroom.RecipeOrderRefundDetailVO;
import recipe.vo.greenroom.RecipeOrderRefundPageVO;
import recipe.vo.greenroom.RecipeOrderRefundReqVO;

/**
 * 退费查询接口调用
 *
 * @author ys
 */
public interface IRecipeOrderRefundService {

    RecipeOrderRefundPageVO findRefundRecipeOrder(RecipeOrderRefundReqVO recipeOrderRefundReqVO);

    RecipeOrderRefundDetailVO getRefundOrderDetail(String orderCode);

    void forceRefund(AuditRefundVO auditRefundVO);
}
