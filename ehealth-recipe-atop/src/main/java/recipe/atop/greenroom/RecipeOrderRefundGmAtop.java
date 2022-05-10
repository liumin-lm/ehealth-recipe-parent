package recipe.atop.greenroom;

import com.ngari.recipe.entity.RecipeRefund;
import ctd.persistence.bean.QueryResult;
import ctd.util.BeanUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.BeanCopyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.greenroom.IRecipeOrderRefundService;
import recipe.vo.greenroom.*;

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
    public RecipeOrderRefundDetailVO getRefundOrderDetail(String orderCode, Integer busType) {
        validateAtop(orderCode);
        return recipeOrderRefundService.getRefundOrderDetail(orderCode, busType);
    }

    @RpcService
    public void forceRefund(AuditRefundVO auditRefundVO) {
        validateAtop(auditRefundVO, auditRefundVO.getOrderCode(), auditRefundVO.getResult());
        recipeOrderRefundService.forceRefund(auditRefundVO);
    }

}
