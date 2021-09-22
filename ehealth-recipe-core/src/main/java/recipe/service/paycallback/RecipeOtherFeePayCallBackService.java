package recipe.service.paycallback;

import com.ngari.recipe.pay.model.PayResultDTO;
import com.ngari.recipe.pay.service.IRecipeOtherFeePayCallBackService;
import ctd.util.annotation.RpcBean;
import eh.entity.bus.Order;

import java.util.Map;

/**
 * 处方其他费用支付回调
 *
 * @author yinsheng
 */
@RpcBean
public class RecipeOtherFeePayCallBackService implements IRecipeOtherFeePayCallBackService {
    @Override
    public boolean doHandleAfterPay(PayResultDTO payResult) {
        return false;
    }

    @Override
    public boolean doHandleAfterPayFail(PayResultDTO payResult) {
        return false;
    }

    @Override
    public boolean doHandleAfterRefund(Order order, int targetPayflag, Map<String, String> refundResult) {
        return false;
    }
}
