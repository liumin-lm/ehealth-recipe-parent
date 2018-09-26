package recipe.service;

import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.wxpay.service.INgariPayService;
import com.ngari.wxpay.service.IUnifiedPayService;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.base.constant.ErrorCode;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.BusTypeEnum;
import recipe.dao.RecipeOrderDAO;

import java.util.Map;

/**
 * 扫码下单相关接口
 * Created by yuanb on 2018/9/19.
 * @author yuanb
 */
@RpcBean("recipeCodeOrderService")
public class RecipeCodeOrderService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RecipeCodeOrderService.class);

    /**
     *
     * @param payway 支付宝扫码支付  32
     * @param orderId
     * @return
     */
    @RpcService
    public String scanCodePayment(String payway ,Integer orderId , String appId){
        //检查业务单状态，如果已经支付成功，直接返回
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder recipeOrder = recipeOrderDAO.get(orderId);
        Integer payFlag = recipeOrder.getPayFlag();
        String outTradeNo = recipeOrder.getOutTradeNo();
        if(payFlag .equals(1)){
            throw new DAOException(ErrorCode.SERVICE_ERROR,"订单已支付，请勿重复下单");
        }
        //判断业务单的商户订单号 ，是否已经下过的呢。如果已经下过单，需要先取消上一个订单
        if(outTradeNo == null || outTradeNo.trim().equals("")){
            IUnifiedPayService unifiedPayService = ApplicationUtils.getBaseService(IUnifiedPayService.class);
            Map<String, Object> cancelmap = unifiedPayService.orderCancel(orderId, BusTypeEnum.RECIPE.getCode());
            String result = (String) cancelmap.get("code");
            String action = (String) cancelmap.get("action");
            logger.info("result:{}, action:{}", result, action);
        }

        // 获取支付的二维码
        INgariPayService payService = ApplicationUtils.getBaseService(INgariPayService.class);
        Map<String, Object> map = payService.appOrder(appId, payway, BusTypeEnum.MEETCLINIC.getCode(), String.valueOf(orderId), "");
        if (map == null || map.get("qr_code") == null) {
            logger.info("payService qr_code is required,or map is null...orderId=" + orderId + ",payway=" + payway + ",appId=" + appId);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "生成二维码失败，请重试");
        }
        String url = (String) map.get("qr_code");
        logger.info("处方订单生成二维码成功：[{}]",url);
        return url;
    }

    /**
     * 查询业务单支付状态
     * @param orderId
     * @return
     */
    @RpcService
    public Integer getPayFlag(Integer orderId){
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder recipeOrder = recipeOrderDAO.get(orderId);
        Integer payFlag = recipeOrder.getPayFlag();
        return payFlag;
    }
}
