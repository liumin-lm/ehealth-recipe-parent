package recipe.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.his.recipe.mode.RecipeRefundReqTO;
import com.ngari.his.recipe.mode.RecipeRefundResTO;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RefundResultDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.wxpay.service.INgariRefundService;
import coupon.api.service.ICouponBaseService;
import coupon.api.vo.Coupon;
import ctd.account.UserRoleToken;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.context.Context;
import ctd.util.context.ContextUtils;
import easypay.entity.vo.param.CommonParam;
import easypay.entity.vo.param.OrderQueryParam;
import eh.utils.MapValueUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.constant.PayServiceConstant;
import recipe.enumerate.status.PayWayEnum;
import recipe.third.IEasyPayServiceInterface;
import recipe.util.JsonUtil;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @description： 支付相关client
 * @author： 刘敏
 * @date： 2022-06-24 14:22
 */
@Service
public class PayClient extends BaseClient {
    @Autowired
    private ICouponBaseService couponService;
    @Autowired
    private INgariRefundService refundService;

    /**
     * 获取优惠券
     *
     * @param couponId 优惠券ID
     * @param totalFee 总金额
     * @return 优惠券
     */
    public Coupon getCouponById(Integer couponId, BigDecimal totalFee) {
        logger.info("PayClient getCouponById couponId:{}, totalFee:{}.", couponId, totalFee);
        if (null == couponId || couponId <= 0) {
            return null;
        }
        Coupon coupon = couponService.lockCouponById(couponId, totalFee);
        logger.info("PayClient getCouponById coupon:{}.", JSONUtils.toString(coupon));
        return coupon;
    }

    /**
     * 获取订单支付状态
     *
     * @param
     * @return
     */
    @LogRecord
    public String orderQuery(RecipeOrder recipeOrder) {
        String tradeStatus = "";

        // 1.获取参数
        // 1.1获取payWay
        PayWayEnum payWayEnum = PayWayEnum.fromCode(recipeOrder.getWnPayWay());
        if (Objects.isNull(payWayEnum)) {
            throw new ServiceException("当前订单无法获取到支付方式！");
        }

        // 1.2获取userId, clientId
        String userId = "";
        String clientId;
        String lastIpAddress = "";
        Map<String, Object> request = ContextUtils.get(Context.RPC_INVOKE_HEADERS, Map.class);
        UserRoleToken token = UserRoleToken.getCurrent();
        if (token != null) {
            userId = StringUtils.defaultString(token.getUserId());
            lastIpAddress = StringUtils.defaultString(token.getLastIPAddress());
        }
        if (request != null) {
            lastIpAddress = StringUtils.defaultString((String) request.get(Context.CLIENT_IP_ADDRESS));
        }

        // 客户端存在代理的情况下，会获取到多个IP地址，此时取第一个IP
        if (lastIpAddress.contains(",")) {
            clientId = lastIpAddress.split(",")[0];
        } else {
            clientId = lastIpAddress;
        }
        OrderQueryParam orderQueryParam = new OrderQueryParam();
        orderQueryParam.setApplyNo(recipeOrder.getOutTradeNo());
//        orderQueryParam.setTradeNo(recipeOrder.getTradeNo());

        CommonParam commonParam = new CommonParam();
        commonParam.setOrganId(recipeOrder.getOrderId() + "");
        // 1 支付宝；2 微信；3 一网通
        commonParam.setPayType(payWayEnum.getPayType());
        commonParam.setService(PayServiceConstant.ORDER_QUERY);
        commonParam.setUserId(userId);
        // 用户客户端IP地址
        commonParam.setClientId(clientId);
        commonParam.setSign("");
        commonParam.setBizParam(net.sf.json.JSONObject.fromObject(orderQueryParam));
        String result = "";
        // 3.调用2.2.订单状态查询(order.query)
        try {
            logger.info("order.query commonParam={}", JSON.toJSONString(commonParam));
            IEasyPayServiceInterface payService = AppContextHolder.getBean("easypay.payService", IEasyPayServiceInterface.class);
            result = payService.gateWay(commonParam);
            logger.info("order.query result={}", JsonUtil.toString(result));
            JSONObject jsonObject = JSONObject.parseObject(result);
            String code = (String) jsonObject.get("code");
            String msg = (String) jsonObject.get("msg");
            Map<String, Object> resultMap = new HashMap<String, Object>();
            if (code != null && code.equals("200")) {
                //WAIT_BUYER_PAY（交易等待支付）、CLOSED（未付款交易超时关闭，或支付完成后全额退款）、SUCCESS（交易支付成功）、FINISHED（交易结束，不可退款）
                tradeStatus = (String) jsonObject.getJSONObject("data").get("trade_status");
            } else {
                //支付平台异常，调用失败
                logger.info("order.query 掉用失败");
            }
            logger.info("返回支付平台查询结果：" + resultMap);
        } catch (Exception e) {
            tradeStatus = "ERROR";
            logger.error("order.query 请求支付平台服务异常！", e);
        }
        return tradeStatus;
    }


    /**
     * 退费
     *
     * @param orderId 订单ID
     * @param busType 业务类型
     * @return 退费返回
     */
    public RefundResultDTO refund(Integer orderId, String busType) {
        logger.info("RefundClient refund orderId:{},busType:{}.", orderId, busType);
        RefundResultDTO refundResultDTO = new RefundResultDTO();
        try {
            Map<String, Object> refundResult = refundService.refund(orderId, busType);
            if (null != refundResult) {
                refundResultDTO.setStatus(MapValueUtil.getInteger(refundResult, "status"));
                refundResultDTO.setRefundId(MapValueUtil.getString(refundResult, "refund_id"));
                refundResultDTO.setRefundAmount(MapValueUtil.getString(refundResult, "refund_amount"));
            }
        } catch (Exception e) {
            logger.error("RefundClient refund error orderId:{}", orderId, e);
        }
        logger.info("RefundClient refund refundResultDTO:{}.", JSONUtils.toString(refundResultDTO));
        return refundResultDTO;
    }


    /**
     * 处方退款推送his服务
     */
    public String recipeRefund(Recipe recipe, List<Recipedetail> details, PatientDTO patient, HealthCardBean card) {
        RecipeRefundReqTO requestTO = new RecipeRefundReqTO();
        if (null != recipe) {
            requestTO.setOrganID(String.valueOf(recipe.getClinicOrgan()));
            requestTO.setPatId(recipe.getPatientID());
        }

        if (CollectionUtils.isNotEmpty(details)) {
            requestTO.setInvoiceNo(details.get(0).getPatientInvoiceNo());
        }

        if (null != patient) {
            requestTO.setCertID(patient.getCertificate());
            requestTO.setCertificateType(patient.getCertificateType());
            requestTO.setPatientName(patient.getPatientName());
            requestTO.setPatientSex(patient.getPatientSex());
            requestTO.setMobile(patient.getMobile());
        }

        if (null != card) {
            requestTO.setCardType(card.getCardType());
            requestTO.setCardNo(card.getCardId());
        }
        requestTO.setHoscode("");
        requestTO.setEmpId("");
        logger.info("RefundClient recipeRefund recipeRefund request:{}.", JSONUtils.toString(requestTO));
        try {
            RecipeRefundResTO response = recipeHisService.recipeRefund(requestTO);
            logger.info("RefundClient recipeRefund response={}", JSONUtils.toString(response));
            if (null == response || null == response.getMsgCode()) {
                return "response is null";
            }
            if (0 != response.getMsgCode()) {
                return response.getMsg();
            }
            return "成功";
        } catch (Exception e) {
            logger.info("RefundClient recipeRefund error ", e);
            return e.getMessage();
        }
    }
}
