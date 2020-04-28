package recipe.serviceprovider.recipeorder.service;

import com.ngari.recipe.common.RecipeBussResTO;
import com.ngari.recipe.common.RecipeListReqTO;
import com.ngari.recipe.common.RecipeListResTO;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import com.ngari.recipe.recipeorder.service.IRecipeOrderService;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.billcheck.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.dao.RecipeOrderDAO;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.service.RecipeOrderService;
import recipe.serviceprovider.BaseService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.*;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/9/5.
 */
@RpcBean("remoteRecipeOrderService")
public class RemoteRecipeOrderService extends BaseService<RecipeOrderBean> implements IRecipeOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeOrderService.class);
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
    @RpcService
    public Map<String, Object> recipeOrderDetailedStatistics(Date startTime, Date endTime, Integer organId, List<Integer> organIds, Integer depId, Integer drugId, String orderColumn, String orderType, int start, int limit){
        List<Map<String, Object>> list = DAOFactory.getDAO(RecipeOrderDAO.class).queryrecipeOrderDetailed(startTime, endTime, organId, organIds, depId, drugId, orderColumn, orderType, start, limit);
        Map<String, Object> map = DAOFactory.getDAO(RecipeOrderDAO.class).queryrecipeOrderDetailedTotal(startTime, endTime, organId, organIds, depId, drugId);
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
}
