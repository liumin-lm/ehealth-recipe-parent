package recipe.serviceprovider.recipeorder.service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.recipe.common.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeRefundBean;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import com.ngari.recipe.recipeorder.service.IRecipeOrderService;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.billcheck.vo.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.ThirdResultBean;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBaseTrackingStatusEnum;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.RefundNodeStatusConstant;
import recipe.dao.*;
import recipe.drugsenterprise.ThirdEnterpriseCallService;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.service.*;
import recipe.serviceprovider.BaseService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/9/5.
 */
@RpcBean("remoteRecipeOrderService")
public class RemoteRecipeOrderService extends BaseService<RecipeOrderBean> implements IRecipeOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteRecipeOrderService.class);

    @RpcService
    @Override
    public RecipeOrderBean get(Object id) {
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = orderDAO.get(id);
        return getBean(order, RecipeOrderBean.class);
    }

    @RpcService
    @Override
    public RecipeBussResTO<RecipeOrderBean> createBlankOrder(List<Integer> recipeIds, Map<String, String> map) {
        RecipeOrderService service = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeOrderBean order = service.createBlankOrder(recipeIds, map);
        return RecipeBussResTO.getSuccessResponse(order);
    }

    @RpcService
    @Override
    public void finishOrderPay(String orderCode, int payFlag, Integer payMode) {
        RecipeOrderService service = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        service.finishOrderPay(orderCode, payFlag, payMode);
        RecipeBusiThreadPool.submit(()->{
            HisSyncSupervisionService  hisSyncservice = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
            hisSyncservice.uploadRecipePayToRegulation(orderCode,payFlag);
            return null;
        });
    }

    @RpcService
    @Override
    public BigDecimal countOrderTotalFeeWithCoupon(BigDecimal actualPrice, RecipeOrderBean recipeOrderBean) {
        RecipeOrder order = getBean(recipeOrderBean, RecipeOrder.class);
        RecipeOrderService service = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        return service.countOrderTotalFeeWithCoupon(actualPrice, order);
    }

    @RpcService
    @Override
    public void updateOrderInfo(String orderCode, Map<String, Object> map) {
        LOGGER.info("RemoteRecipeOrderService updateOrderInfo orderCode={}, map={}", orderCode, JSONUtils.toString(map));
        RecipeOrderService service = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        service.updateOrderInfo(orderCode, map, null);
    }

    @RpcService
    @Override
    public RecipeOrderBean getOrderByRecipeId(int recipeId) {
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = recipeOrderDAO.getOrderByRecipeId(recipeId);
        return getBean(order, RecipeOrderBean.class);
    }

    @RpcService
    @Override
    public RecipeOrderBean getByOutTradeNo(String outTradeNo) {
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = recipeOrderDAO.getByOutTradeNo(outTradeNo);
        return getBean(order, RecipeOrderBean.class);
    }

    @Override
    public RecipeOrderBean getByOrderCode(String orderCode) {
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = recipeOrderDAO.getByOrderCode(orderCode);
        return getBean(order, RecipeOrderBean.class);
    }

    @RpcService
    @Override
    public RecipeListResTO<RecipeOrderBean> findByPayFlag(RecipeListReqTO request) {
        Integer payFlag = MapValueUtil.getInteger(request.getConditions(), "payFlag");
        if (null == payFlag) {
            return RecipeListResTO.getFailResponse("缺少payFlag参数");
        }

        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        List<RecipeOrder> orderList = orderDAO.findByPayFlag(payFlag);
        List<RecipeOrderBean> backList = getList(orderList, RecipeOrderBean.class);
        return RecipeListResTO.getSuccessResponse(backList);
    }

    @RpcService
    @Override
    public BigDecimal reCalculateRecipeFee(Integer enterpriseId, List<Integer> recipeIds, Map<String, String> extInfo) {
        RecipeOrderService service = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        return service.reCalculateRecipeFee(enterpriseId,recipeIds,extInfo);
    }

    @RpcService
    @Override
    public RecipeOrderBean getRelationOrderByRecipeId(int recipeId) {
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = recipeOrderDAO.getRelationOrderByRecipeId(recipeId);
        return getBean(order, RecipeOrderBean.class);
    }

    @Override
    public RecipeBillResponse<BillRecipeDetailVo> getRecipePayInfoByDate(RecipeBillRequest request) {
//        List<BillRecipeDetailVo> list = new ArrayList<BillRecipeDetailVo>();
        RecipeBillResponse<BillRecipeDetailVo> rep = new RecipeBillResponse<BillRecipeDetailVo>();
        if(request == null){
            LOGGER.error("参数不能为空");
            return null;
        }
        if(request.getStartTime() == null){
            LOGGER.error("开始时间不能为空");
            return null;
        }
        if(request.getEndTime() == null){
            LOGGER.error("结束时间不能为空");
            return null;
        }

        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        List<BillRecipeDetailVo> list = recipeOrderDAO.getPayAndRefundInfoByTime(request.getStartTime(), request.getEndTime(),request.getStart(),request.getPageSize());

        rep.setStart(request.getStart());
        rep.setPageSize(request.getPageSize());
        rep.setTotal(list.size());
        rep.setData(list);
//        for(int i= 0; i<list.size(); i++){
//            BillRecipeDetailVo vo = new BillRecipeDetailVo();
//            RecipeOrder order = list.get(i);
//            vo.setBillType(1);
//            vo.setOutTradeNo(order.getOutTradeNo();
//            vo.setRecipeId(order.getrec);
//            vo.setMpiId(order.getMpiId());
//            vo.setDoctorId(order.getdoc);
//            vo.setRecipeTime(order.getPayTime());
//            vo.setOrganId(order.getOrganId());
//            vo.setDeptId(order.getEnterpriseId());
//            vo.setSettleType(order.getOrderType());
//            vo.setDeliveryMethod(order.getGiveMode());
//            vo.setDrugCompany(order.getEnterpriseId());
//            vo.setDrugCompanyName(order.getEnterpriseName());
//            vo.setPayFlag(order.getPayFlag());
//            vo.setAppointFee(order.getRegisterFee().doubleValue());
//            vo.setDeliveryFee(order.getExpressFee().doubleValue());
//            vo.setDaiJianFee(order.getDecoctionFee().doubleValue());
//            vo.setReviewFee(order.getAuditFee().doubleValue());
//            vo.setOtherFee(order.getOtherFee().doubleValue());
//            vo.setDrugFee(order.getRecipeFee().doubleValue());
//            vo.setDicountedFee(order.getCouponFee().doubleValue());
//            vo.setTotalFee(order.getTotalFee().doubleValue());
//            vo.setMedicarePay(order.getFundAmount());
//            vo.setSelfPay(order.getTotalFee().subtract(new BigDecimal(order.getFundAmount())).doubleValue());
//     }
        return rep;
    }

    /**
     * 根据日期获取电子处方药企配送订单明细
     *
     * @param startTime 开始时间
     * @param endTime 截止时间
     * @param organId 机构ID
     * @param depId 药企ID
     * @return RecipeOrderBean
     */
    @Override
    @RpcService
    public Map<String, Object> recipeOrderDetailedStatistics(Date startTime, Date endTime, Integer organId, List<Integer> organIds, Integer depId, Integer drugId, String orderColumn, String orderType,Integer recipeId, Integer payType, int start, int limit){
        List<Map<String, Object>> list = DAOFactory.getDAO(RecipeOrderDAO.class).queryrecipeOrderDetailed(startTime, endTime, organId, organIds, depId, drugId, orderColumn, orderType, recipeId,payType,start, limit);
        Map<String, Object> map = DAOFactory.getDAO(RecipeOrderDAO.class).queryrecipeOrderDetailedTotal(startTime, endTime, organId, organIds, depId, drugId,recipeId, payType);
        map.put("orderData", list);
        return map;
    }

    /**
     * 电子处方药企配送药品统计
     *
     * @param startTime 开始时间
     * @param endTime 截止时间
     * @param organId 机构ID
     * @param depId 药企ID
     * @return RecipeOrderBean
     */
    @Override
    @RpcService
    public Map<String, Object> recipeDrugStatistics(Date startTime, Date endTime, Integer organId, List<Integer> organIds, Integer depId, Integer recipeId, String orderColumn, String orderType, int start, int limit){
        List<Map<String, Object>> list = DAOFactory.getDAO(RecipeOrderDAO.class).queryrecipeDrug(startTime, endTime, organId, organIds, depId, recipeId, orderColumn, orderType, start, limit);
        Map<String, Object> map = DAOFactory.getDAO(RecipeOrderDAO.class).queryrecipeDrugtotal(startTime, endTime, organId, organIds, depId, recipeId);
        map.put("drugData", list);
        return map;
    }

    @Override
    public List<BillBusFeeVo> findRecipeFeeList(RecipeBillRequest recipeBillRequest) {
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        return recipeOrderDAO.findRecipeFeeList(recipeBillRequest);
    }

    @Override
    public List<BillDrugFeeVo> findDrugFeeList(RecipeBillRequest recipeBillRequest) {
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        return recipeOrderDAO.findDrugFeeList(recipeBillRequest);
    }

    @Override
    public RecipeRefundBean getRecipeRefundByRecipeIdAndNode(Integer recipeId, Integer node) {
        RecipeRefundDAO recipeRefundDAO = DAOFactory.getDAO(RecipeRefundDAO.class);
        List<RecipeRefund> refunds = recipeRefundDAO.findRecipeRefundByRecipeIdAndNode(recipeId, node);
        return getBean(refunds.get(0), RecipeRefundBean.class);
    }

    @Override
    public void refundCallback(Integer busId, Integer refundStatus, String msg){
        LOGGER.info("RemoteRecipeOrderService.refundCallback busId:{},refundStatus:{},msg:{}.", busId, refundStatus, msg);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(busId);
        if(null == recipe){
            LOGGER.warn("当前处方{}不存在无法退费！", busId);
            return;
        }
        //判断当前处方是不是有审核通过的患者手动弄退费信息，有的话设置处方和订单的状态
        RecipeRefundDAO recipeRefundDAO = DAOFactory.getDAO(RecipeRefundDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder recipeOrder = recipeOrderDAO.getRecipeOrderByRecipeId(busId);
        if(null == recipeOrder){
            LOGGER.warn("当前处方订单{}不存在无法退费！", busId);
            return;
        }
        RecipeRefundService recipeRefundService = ApplicationUtils.getRecipeService(RecipeRefundService.class);

        RecipeRefund nowRecipeRefund = new RecipeRefund();
        nowRecipeRefund.setTradeNo(recipeOrder.getTradeNo());
        nowRecipeRefund.setPrice(recipeOrder.getActualPrice());
        nowRecipeRefund.setNode(9);
        nowRecipeRefund.setStatus(refundStatus);
        nowRecipeRefund.setReason(msg);
        //根据业务id，根据退费推送消息
        //当退费成功后修改处方和订单的状态
        switch (refundStatus) {
            case 3:
                List<RecipeRefund> recipeRefunds = recipeRefundDAO.findRefundListByRecipeIdAndNodes(busId, Arrays.asList(9));
                LOGGER.info("退款完成开始处理：{}",recipe.getRecipeId());
                if (CollectionUtils.isNotEmpty(recipeRefunds)) {
                    return;
                }
                RecipeMsgService.batchSendMsg(busId, RecipeStatusConstant.RECIPE_REFUND_SUCC);
                //修改处方单状态 处理合并支付
                List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
                List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIdList);
                recipes.forEach(recipe1 -> {
                    recipeDAO.updateRecipeInfoByRecipeId(recipe1.getRecipeId(), RecipeStatusConstant.REVOKE, ImmutableMap.of("payFlag",3));
                    LOGGER.info("退款完成修改处方状态：{}",recipe1.getRecipeId());
                });
                //订单状态修改
                Map<String, Object> orderAttrMap = Maps.newHashMap();
                orderAttrMap.put("effective", 0);
                orderAttrMap.put("status", OrderStatusConstant.CANCEL_MANUAL);
                //修改支付flag的状态，退费信息
                orderAttrMap.put("payFlag", 3);
                orderAttrMap.put("refundFlag", 1);
                orderAttrMap.put("refundTime", new Date());
                recipeOrderDAO.updateByOrdeCode(recipeOrder.getOrderCode(), orderAttrMap);
                LOGGER.info("退款完成修改订单状态：{}",recipe.getRecipeId());
                RecipeLogService.saveRecipeLog(busId, recipe.getStatus(), RecipeStatusConstant.REVOKE, msg);
                LOGGER.info("存储退款完成记录-remoteRecipeOrderService：{}",recipe.getRecipeId());
                recipeRefundService.recipeReFundSave(recipe, nowRecipeRefund);
                break;
            case 4:
                nowRecipeRefund.setReason("退费失败");
                RecipeMsgService.batchSendMsg(busId, RecipeStatusConstant.RECIPE_REFUND_FAIL);
                RecipeLogService.saveRecipeLog(busId, recipe.getStatus(), recipe.getStatus(), msg);
                recipeRefundService.recipeReFundSave(recipe, nowRecipeRefund);
                recipeRefundService.updateRecipeRefundStatus(recipe, RefundNodeStatusConstant.REFUND_NODE_FAIL_AUDIT_STATUS);
                break;
            default:
                LOGGER.warn("当前处方{}退费状态{}无法解析！", busId, refundStatus);
                break;
        }


    }

    @Override
    public Boolean updatePharmNo(Integer recipeId, String pharmNo) {
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        return recipeDetailDAO.updateRecipeDetailByRecipeId(recipeId, ImmutableMap.of("pharmNo", pharmNo));
    }

    @Override
    public Boolean updatePharmNoS(List<Integer> recipeId, String pharmNo) {
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        return recipeDetailDAO.updateRecipeDetailByRecipeIdS(recipeId, ImmutableMap.of("pharmNo", pharmNo));
    }

    @Override
    @RpcService
    public Boolean updateRecipeTrannckingInfo(RecipeTrannckingReqTO trannckingReqTO) {
        LOGGER.info("updateRecipeTrannckingInfo.req={}", JSONObject.toJSONString(trannckingReqTO));
        if (StringUtils.isBlank(trannckingReqTO.getLogisticsCompany()) || StringUtils.isBlank(trannckingReqTO.getTrackingNumber()) || null == trannckingReqTO.getTrackingStatus()){
            throw new DAOException(DAOException.VALUE_NEEDED,"物流公司、编号、状态值不能为空");
        }
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        String orderCode = recipeOrderDAO.getOrderCodeByLogisticsCompanyAndTrackingNumber(Integer.parseInt(trannckingReqTO.getLogisticsCompany()),trannckingReqTO.getTrackingNumber());
        LOGGER.info("updateRecipeTrannckingInfo.queryRecipeOrderCode={}",orderCode);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        try {
            if(StringUtils.isNotBlank(orderCode)){
                List<Recipe> recipeList = recipeDAO.findRecipeListByOrderCode(orderCode);
                LOGGER.info("updateRecipeTrannckingInfo.queryRcipe={}",JSONObject.toJSONString(recipeList));
                if(recipeList.size() > 0){
                    Recipe recipe = recipeList.get(0);
                    RecipeBaseTrackingStatusEnum statusEnum = RecipeBaseTrackingStatusEnum.getByBaseCode(trannckingReqTO.getTrackingStatus());
                    if (null != statusEnum){
                        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
                        RecipeOrder order = orderDAO.getOrderByRecipeId(recipe.getRecipeId());
                        if (statusEnum.getRecipeCode().equals(order.getStatus())){
                            return true;
                        }
                        Map<String, Object> paramMap = new HashedMap();
                        paramMap.put("recipeId",recipe.getRecipeId());
                        paramMap.put("sendDate",trannckingReqTO.getSendDate());
                        paramMap.put("sender",trannckingReqTO.getSender());
                        paramMap.put("logisticsCompany",trannckingReqTO.getLogisticsCompany());
                        paramMap.put("trackingNumber",trannckingReqTO.getTrackingNumber());
                        LOGGER.info("updateRecipeTrannckingInfo.updateTrannckingInfo={}",JSONObject.toJSONString(paramMap));
                        ThirdEnterpriseCallService callService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
                        ThirdResultBean sendCallResult = null;
                        switch (statusEnum.getRecipeCode()){
                            case 3:
                                // 待配送
                                sendCallResult = callService.readyToSend(paramMap);
                                break;
                           case 4:
                               // 配送中
                               sendCallResult = callService.toSend(paramMap);
                               break;
                           case 5:
                               // 配送完成
                               paramMap.put("recipeCode",recipe.getRecipeCode());
                               paramMap.put("sendDate",trannckingReqTO.getFinishDate());
                               sendCallResult = callService.finishRecipe(paramMap);
                               break;
                           default:
                               break;
                       }
                        LOGGER.info("updateRecipeTrannckingInfo.updateResult={}",JSONObject.toJSONString(sendCallResult));
                        if (sendCallResult != null && 200 == sendCallResult.getCode()){
                            return true;
                        }
                    }else {
                        LOGGER.info("updateRecipeTrannckingInfo.statusEnum is null not update");
                        return true;
                    }
                }
            }else {
                throw new DAOException(DAOException.VALUE_NEEDED,"查询不到处方订单");
            }
        } catch (Exception e) {
            LOGGER.error("updateRecipeTrannckingInfo.error:",e);
        }

        return false;
    }

    @Override
    @RpcService
    public Boolean saveRecipeOrderBill(RecipeOrderBillReqTO orderBillReqTO) {
        LOGGER.info("保存处方订单电子票据入参={}",JSONObject.toJSONString(orderBillReqTO));
        RecipeOrderBillDAO orderBillDAO = DAOFactory.getDAO(RecipeOrderBillDAO.class);
        RecipeOrderBill bill = new RecipeOrderBill();
        bill.setRecipeOrderCode(orderBillReqTO.getRecipeOrderCode());
        bill.setBillPictureUrl(orderBillReqTO.getBillPictureUrl());
        bill.setBillQrCode(orderBillReqTO.getBillQrCode());
        bill.setBillBathCode(orderBillReqTO.getBillBathCode());
        bill.setBillNumber(orderBillReqTO.getBillNumber());
        bill.setCreateTime(new Date());
        LOGGER.info("保存处方订单电子票据信息={}",JSONObject.toJSONString(bill));
        orderBillDAO.save(bill);
        return true;
    }

    @Override
    @RpcService
    public Boolean updateExtPharmNoS(List<Integer> recipeId, String pharmNo) {
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        return recipeExtendDAO.updateRecipeExtByRecipeIdS(recipeId, ImmutableMap.of("pharmNo", pharmNo));
    }

}
