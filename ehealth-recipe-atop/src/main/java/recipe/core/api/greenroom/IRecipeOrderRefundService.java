package recipe.core.api.greenroom;

import com.ngari.recipe.entity.RecipeRefund;
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

    RecipeOrderRefundDetailVO getRefundOrderDetail(String orderCode, Integer busType);

    void forceRefund(AuditRefundVO auditRefundVO);

    RecipeRefund findApplyRefund(Integer recipeId);
}
