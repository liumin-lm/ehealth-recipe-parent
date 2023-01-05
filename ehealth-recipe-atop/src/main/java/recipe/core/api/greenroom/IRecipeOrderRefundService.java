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

    RecipeOrderRefundPageVO findRefundRecipeOrder(RecipeOrderRefundReqVO recipeOrderRefundReqVO);

    RecipeOrderRefundDetailVO getRefundOrderDetail(String orderCode, Integer busType);

    void forceRefund(AuditRefundVO auditRefundVO);

    RecipeRefund findApplyRefund(Integer recipeId);

    void updateRecipePushFlag(List<Integer> recipeIds);
}
