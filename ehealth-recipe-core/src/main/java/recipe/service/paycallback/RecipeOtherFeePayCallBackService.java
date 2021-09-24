package recipe.service.paycallback;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.RecipeOrderPayFlow;
import com.ngari.recipe.pay.model.PayResultDTO;
import com.ngari.recipe.pay.service.IRecipeOtherFeePayCallBackService;
import ctd.util.annotation.RpcBean;
import ctd.util.converter.ConversionUtils;
import eh.entity.bus.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.enumerate.type.PayFlagEnum;
import recipe.enumerate.type.PayFlowTypeEnum;
import recipe.manager.OrderManager;
import recipe.manager.RecipeManager;
import recipe.manager.RecipeOrderPayFlowManager;
import recipe.service.RecipeOrderService;

import java.util.Map;

/**
 * 处方其他费用支付回调
 *
 * @author yinsheng
 */
@RpcBean
public class RecipeOtherFeePayCallBackService implements IRecipeOtherFeePayCallBackService {

    private static final Logger logger = LoggerFactory.getLogger(RecipeOtherFeePayCallBackService.class);

    @Autowired
    private OrderManager orderManager;

    @Autowired
    private RecipeOrderPayFlowManager recipeOrderPayFlowManager;

    @Autowired
    private RecipeOrderService orderService;

    @Autowired
    private RecipeManager recipeManager;



    @Override
    public boolean doHandleAfterPay(PayResultDTO payResult) {
        logger.info("RecipeOtherFeePayCallBackService doHandleAfterPay payResult:{}.", JSON.toJSONString(payResult));
        Integer busId = payResult.getBusId();
        String outTradeNo = payResult.getOutTradeNo();
        String tradeNo = payResult.getTradeNo();
        String payOrderId = payResult.getPayOrganId();
        Map<String, String> notifyMap = payResult.getNotifyMap();

        RecipeOrder order = orderManager.getRecipeOrderById(busId);
        if (null == order) {
            logger.info("RecipeOtherFeePayCallBackService doHandleAfterPay busObject not exists, busId[{}]", busId);
            return false;
        }
        RecipeOrderPayFlow recipeOrderPayFlow = recipeOrderPayFlowManager.getByOrderIdAndType(busId, PayFlowTypeEnum.RECIPE_AUDIT.getType());
        if (null == recipeOrderPayFlow) {
            recipeOrderPayFlow = new RecipeOrderPayFlow();
            recipeOrderPayFlow.setOrderId(order.getOrderId());
            if (null != notifyMap && notifyMap.get("total_amount") != null) {
                recipeOrderPayFlow.setTotalFee(Double.parseDouble(payResult.getNotifyMap().get("total_amount")));
            }
            recipeOrderPayFlow.setPayFlowType(PayFlowTypeEnum.RECIPE_AUDIT.getType());
            recipeOrderPayFlow.setPayFlag(PayFlagEnum.PAYED.getType());
            recipeOrderPayFlow.setOutTradeNo(payResult.getOutTradeNo());
            recipeOrderPayFlow.setPayOrganId(payResult.getPayOrganId());
            recipeOrderPayFlow.setTradeNo(payResult.getTradeNo());
            recipeOrderPayFlow.setWnPayWay("");
            recipeOrderPayFlow.setWxPayWay(payResult.getPayWay());
            recipeOrderPayFlowManager.save(recipeOrderPayFlow);
        } else {
            recipeOrderPayFlow.setPayFlag(PayFlagEnum.PAYED.getType());
            recipeOrderPayFlow.setOutTradeNo(outTradeNo);
            recipeOrderPayFlow.setTradeNo(tradeNo);
            recipeOrderPayFlow.setPayOrganId(payOrderId);
            if (notifyMap != null && notifyMap.get("total_amount") != null) {
                Double payBackPrice = ConversionUtils.convert(notifyMap.get("total_amount"), Double.class);
                recipeOrderPayFlow.setTotalFee(payBackPrice);
            }
            recipeOrderPayFlowManager.updateNonNullFieldByPrimaryKey(recipeOrderPayFlow);
        }

        //如果不需要支付处方费用则订单直接完成

        RecipeOrder recipeOrder = orderManager.getRecipeOrderById(busId);
        if (null == recipeOrder) {
            return false;
        }
        Recipe recipe = recipeManager.getByRecipeCodeAndClinicOrgan(recipeOrder.getOrderCode(), recipeOrder.getOrganId());
        Integer giveMode = recipe.getGiveMode();
        if (new Integer(2).equals(recipeOrder.getPayMode())) {
            //表示该处方为线下付款
            Integer payMode = 2;
            switch (giveMode) {
                case 2:
                    payMode = 3;
                    break;
                case 3:
                    payMode = 4;
                    break;
                default:
                    break;
            }
            orderService.finishOrderPay(recipeOrder.getOrderCode(), PayFlagEnum.PAYED.getType(), payMode);
        }
        return true;
    }

    @Override
    public boolean doHandleAfterPayFail(PayResultDTO payResult) {
        logger.info("RecipeOtherFeePayCallBackService doHandleAfterPayFail payResult:{}.", JSON.toJSONString(payResult));
        return true;
    }

    @Override
    public boolean doHandleAfterRefund(Order order, int targetPayFlag, Map<String, String> refundResult) {
        logger.info("RecipeOtherFeePayCallBackService doHandleAfterRefund order:{},targetPayFlag:{},refundResult:{}.", JSON.toJSONString(order), targetPayFlag, JSON.toJSONString(refundResult));
        RecipeOrderPayFlow recipeOrderPayFlow = recipeOrderPayFlowManager.getByOutTradeNo(order.getOutTradeNo());
        StringBuilder memo = new StringBuilder("订单=");
        memo.append(recipeOrderPayFlow.getOrderId()).append(" ");
        switch (targetPayFlag) {
            case 3:
                memo.append("退款成功");
                recipeOrderPayFlow.setPayFlag(PayFlagEnum.REFUND_SUCCESS.getType());
                break;
            case 4:
                memo.append("退款失败");
                recipeOrderPayFlow.setPayFlag(PayFlagEnum.REFUND_FAIL.getType());
                break;
            default:
                memo.append("支付 未知状态，payFlag:" + targetPayFlag);
                break;
        }
        logger.info("RecipeOtherFeePayCallBackService doHandleAfterRefund memo:{}.", memo.toString());
        return recipeOrderPayFlowManager.updateNonNullFieldByPrimaryKey(recipeOrderPayFlow);
    }
}
