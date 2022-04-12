package recipe.atop.greenroom;

import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.greenroom.IRecipeOrderRefundService;
import recipe.vo.greenroom.AuditRefundVO;
import recipe.vo.greenroom.RecipeOrderRefundDetailVO;
import recipe.vo.greenroom.RecipeOrderRefundPageVO;
import recipe.vo.greenroom.RecipeOrderRefundReqVO;

/**
 * 处方退费运营平台操作查询接口
 */
@RpcBean(value = "recipeOrderRefundGmAtop", mvc_authentication = false)
public class RecipeOrderRefundGmAtop extends BaseAtop {

    @Autowired
    private IRecipeOrderRefundService recipeOrderRefundService;

    @RpcService
    public RecipeOrderRefundPageVO findRefundRecipeOrder(RecipeOrderRefundReqVO recipeOrderRefundReqVO) {
        validateAtop(recipeOrderRefundReqVO, recipeOrderRefundReqVO.getBeginTime(), recipeOrderRefundReqVO.getEndTime());
        return recipeOrderRefundService.findRefundRecipeOrder(recipeOrderRefundReqVO);
    }

    @RpcService
    public RecipeOrderRefundDetailVO getRefundOrderDetail(String orderCode) {
        validateAtop(orderCode);
        return recipeOrderRefundService.getRefundOrderDetail(orderCode);
    }

    @RpcService
    public void forceRefund(AuditRefundVO auditRefundVO) {
        validateAtop(auditRefundVO, auditRefundVO.getOrderCode(), auditRefundVO.getResult());

    }

}
