package recipe.purchase;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DepListBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.RecipePayModeSupportBean;
import recipe.constant.*;
import recipe.dao.*;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.service.RecipeOrderService;
import recipe.service.RecipeServiceSub;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 在线支付-配送到家购药方式
 * @version： 1.0
 */
public class PayModeOnline implements IPurchaseService {
    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PayModeOnline.class);

    @Override
    public RecipeResultBean findSupportDepList(Recipe dbRecipe, Map<String, String> extInfo) {
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        DepListBean depListBean = new DepListBean();
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

        Integer recipeId = dbRecipe.getRecipeId();

        //药企列表
        List<DepDetailBean> depDetailList = new ArrayList<>();

        //获取购药方式查询列表
        List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(getPayMode());
        List<Integer> payModeSupportDoc = RecipeServiceSub.getDepSupportMode(RecipeBussConstant.PAYMODE_COD);
        payModeSupport.addAll(payModeSupportDoc);
        if (CollectionUtils.isEmpty(payModeSupport)) {
            LOG.warn("findSupportDepList 处方[{}]无法匹配配送方式. payMode=[{}]", recipeId, getPayMode());
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("配送模式配置有误");
            return resultBean;
        }
        LOG.info("drugsEnterpriseList organId:{}, payModeSupport:{}", dbRecipe.getClinicOrgan(), payModeSupport);
        //筛选出来的数据已经去掉不支持任何方式配送的药企
        List<DrugsEnterprise> drugsEnterpriseList =
                drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(dbRecipe.getClinicOrgan(), payModeSupport);
        if (CollectionUtils.isEmpty(drugsEnterpriseList)) {
            LOG.warn("findSupportDepList 处方[{}]没有任何药企可以进行配送！", recipeId);
            resultBean.setCode(5);
            resultBean.setMsg("抱歉，没有可选择的药企");
            return resultBean;
        }

        //处理详情
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        List<Integer> drugIds = new ArrayList<>(detailList.size());
        Map<Integer, Double> drugIdCountMap = Maps.newHashMap();
        for (Recipedetail detail : detailList) {
            drugIds.add(detail.getDrugId());
            drugIdCountMap.put(detail.getDrugId(), detail.getUseTotalDose());
        }

        List<DrugsEnterprise> subDepList = new ArrayList<>(drugsEnterpriseList.size());
        for (DrugsEnterprise dep : drugsEnterpriseList) {
            //药品匹配成功标识
            boolean stockFlag = scanStock(dbRecipe, dep, drugIds);
            if (stockFlag) {
                subDepList.add(dep);
            }
        }

        if (CollectionUtils.isEmpty(subDepList)) {
            LOG.warn("findSupportDepList 该处方无法配送. recipeId=[{}]", recipeId);
            resultBean.setCode(5);
            resultBean.setMsg("抱歉，没有可选择的药企");
            return resultBean;
        }
        subDepList = getAllSubDepList(subDepList);
        DepDetailBean depDetailBean;
        for (DrugsEnterprise dep : subDepList) {
            depDetailBean = new DepDetailBean();
            depDetailBean.setDepId(dep.getId());
            depDetailBean.setDepName(dep.getName());
            depDetailBean.setRecipeFee(dbRecipe.getTotalMoney());
            depDetailBean.setBelongDepName(dep.getName());
            depDetailBean.setOrderType(dep.getOrderType());
            if (RecipeBussConstant.PAYMODE_ONLINE.equals(dep.getPayModeSupport()) || RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS.equals(dep.getPayModeSupport())) {
                depDetailBean.setPayModeText("在线支付");
                depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_ONLINE);
            } else {
                depDetailBean.setPayModeText("货到付款");
                depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_COD);
            }

            //如果是价格自定义的药企，则需要设置单独价格
            if (Integer.valueOf(0).equals(dep.getSettlementMode())) {
                List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(dep.getId(), drugIds);
                if (CollectionUtils.isNotEmpty(saleDrugLists)) {
                    BigDecimal total = BigDecimal.ZERO;
                    try {
                        for (SaleDrugList saleDrug : saleDrugLists) {
                            //保留3位小数
                            total = total.add(saleDrug.getPrice().multiply(new BigDecimal(drugIdCountMap.get(saleDrug.getDrugId())))
                                    .divide(BigDecimal.ONE, 3, RoundingMode.UP));
                        }
                    } catch (Exception e) {
                        LOG.warn("findSupportDepList 重新计算药企ID为[{}]的结算价格出错. drugIds={}", dep.getId(),
                                JSONUtils.toString(drugIds), e);
                        continue;
                    }

                    //重置药企处方价格
                    depDetailBean.setRecipeFee(total);
                }
            }

            depDetailList.add(depDetailBean);
        }
        //此处校验是否存在支持药店配送的药企
        checkStoreForSendToHom(dbRecipe, depDetailList);
        depListBean.setSigle(false);
        if (CollectionUtils.isNotEmpty(depDetailList) && depDetailList.size() == 1) {
            depListBean.setSigle(true);
        }

        depListBean.setList(depDetailList);
        resultBean.setObject(depListBean);
        return resultBean;
    }

    @Override
    public OrderCreateResult order(Recipe dbRecipe, Map<String, String> extInfo) {
        LOG.info("PayModeOnline order recipeId={}",dbRecipe.getRecipeId());
        OrderCreateResult result = new OrderCreateResult(RecipeResultBean.SUCCESS);
        RecipeOrder order = new RecipeOrder();
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);

        Integer recipeId = dbRecipe.getRecipeId();
        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        RecipePayModeSupportBean payModeSupport = orderService.setPayModeSupport(order, payMode);
        Integer depId = MapValueUtil.getInteger(extInfo, "depId");
        String payway = MapValueUtil.getString(extInfo, "payway");

        if (StringUtils.isEmpty(payway)) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("支付信息不全");
            return result;
        }
        order.setWxPayWay(payway);
        //处理详情
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        List<Integer> drugIds = FluentIterable.from(detailList).transform(new Function<Recipedetail, Integer>() {
            @Override
            public Integer apply(Recipedetail input) {
                return input.getDrugId();
            }
        }).toList();

        DrugsEnterprise dep = drugsEnterpriseDAO.get(depId);
        boolean stockFlag = scanStock(dbRecipe, dep, drugIds);
        if (!stockFlag) {
            //无法配送
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("药企无法配送");
            return result;
        } else {
            order.setEnterpriseId(depId);
        }
        order.setRecipeIdList(JSONUtils.toString(Arrays.asList(recipeId)));

        // 暂时还是设置成处方单的患者，不然用户历史处方列表不好查找
        order.setMpiId(dbRecipe.getMpiid());
        order.setOrganId(dbRecipe.getClinicOrgan());
        order.setOrderCode(orderService.getOrderCode(order.getMpiId()));
        String drugStoreCode = MapValueUtil.getString(extInfo, "pharmacyCode");
        if (StringUtils.isNotEmpty(drugStoreCode)) {
            String drugStoreName = MapValueUtil.getString(extInfo, "depName");
            //String DrugStoreAddr = MapValueUtil.
            order.setDrugStoreCode(drugStoreCode);
            order.setDrugStoreName(drugStoreName);
        }
        //设置订单各种费用和配送地址
        List<Recipe> recipeList = Arrays.asList(dbRecipe);
        orderService.setOrderFee(result, order, Arrays.asList(recipeId), recipeList, payModeSupport, extInfo, 1);

        //判断设置状态
        int reviewType = dbRecipe.getReviewType();
        Integer giveMode = dbRecipe.getGiveMode();
        Integer payStatus = null;
        //判断处方是否免费
        if(0 >= order.getActualPrice()){
            //免费不需要走支付
            if(ReviewTypeConstant.Postposition_Check == reviewType){
                payStatus = OrderStatusConstant.READY_CHECK;
            }else{
                payStatus = OrderStatusConstant.READY_SEND;
            }
        }else{
            //走支付，待支付
            payStatus = OrderStatusConstant.READY_PAY;
        }

        order.setExpectSendDate(MapValueUtil.getString(extInfo, "expectSendDate"));
        order.setExpectSendTime(MapValueUtil.getString(extInfo, "expectSendTime"));
        order.setStatus(payStatus);
        order.setStatus(payStatus);

        //设置为有效订单
        order.setEffective(1);
        boolean saveFlag = orderService.saveOrderToDB(order, recipeList, payMode, result, recipeDAO, orderDAO);
        if (!saveFlag) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("订单保存出错");
            return result;
        }

        orderService.setCreateOrderResult(result, order, payModeSupport, 1);
        if(0d >= order.getActualPrice()){
            //如果不需要支付则不走支付
            orderService.finishOrderPay(order.getOrderCode(), 1, MapValueUtil.getInteger(extInfo, "payMode"));
        }else{
            Recipe nowRecipe = recipeDAO.get(recipeId);
            //处方需要支付，需要在确认订单将购药方式绑定上
            if(null == nowRecipe){
                result.setCode(RecipeResultBean.FAIL);
                result.setMsg("当前处方" + recipeId + "不存在！");
                return result;
            }
            nowRecipe.setChooseFlag(1);
            recipeDAO.update(nowRecipe);
        }
        return result;
    }

    @Override
    public Integer getPayMode() {
        return RecipeBussConstant.PAYMODE_ONLINE;
    }

    @Override
    public String getServiceName() {
        return "payModeOnlineService";
    }

    @Override
    public String getTipsByStatusForPatient(Recipe recipe, RecipeOrder order) {
        Integer status = recipe.getStatus();
        String orderCode = recipe.getOrderCode();
        int orderStatus = order.getStatus();
        String tips = "";
        switch (status) {
            case RecipeStatusConstant.CHECK_PASS:
                if (StringUtils.isNotEmpty(orderCode)) {
                    if (orderStatus == OrderStatusConstant.READY_SEND) {
                        tips = "订单已处理，请耐心等待药品配送";
                    }
                }
                break;
            case RecipeStatusConstant.WAIT_SEND:
            case RecipeStatusConstant.CHECK_PASS_YS:
                tips = "处方已审核通过，请耐心等待药品配送";
                break;
            case RecipeStatusConstant.IN_SEND:
                tips = "药企正在配送";
                break;
            case RecipeStatusConstant.FINISH:
                tips = "药企配送完成，订单完成";
                break;
                default:
        }
        return tips;
    }

    @Override
    public Integer getOrderStatus(Recipe recipe) {
        return OrderStatusConstant.READY_SEND;
    }

    private List<DrugsEnterprise> getAllSubDepList(List<DrugsEnterprise> subDepList) {
        List<DrugsEnterprise> returnSubDepList = new ArrayList<>();
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        for (DrugsEnterprise drugsEnterprise : subDepList) {
            returnSubDepList.add(drugsEnterprise);
            if (drugsEnterprise.getPayModeSupport() == 9) {
                DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(drugsEnterprise.getId());
                enterprise.setPayModeSupport(1);
                returnSubDepList.add(enterprise);
            }
        }
        //对货到付款和在线支付进行排序
        Collections.sort(returnSubDepList, new SubDepListComparator());
        return returnSubDepList;
    }

    /**
     * 判断药企库存，包含平台内权限及药企实时库存
     *
     * @param dbRecipe
     * @param dep
     * @param drugIds
     * @return
     */
    private boolean scanStock(Recipe dbRecipe, DrugsEnterprise dep, List<Integer> drugIds) {
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);

        boolean succFlag = false;
        if (null == dep || CollectionUtils.isEmpty(drugIds)) {
            return succFlag;
        }

        //判断药企平台内药品权限，此处简单判断数量是否一致
        Long count = saleDrugListDAO.getCountByOrganIdAndDrugIds(dep.getId(), drugIds);
        if (null != count && count > 0) {
            if (count == drugIds.size()) {
                succFlag = true;
            }
        }

        if (!succFlag) {
            LOG.warn("scanStock 存在不支持配送药品. 处方ID=[{}], 药企ID=[{}], 药企名称=[{}], drugIds={}",
                    dbRecipe.getRecipeId(), dep.getId(), dep.getName(), JSONUtils.toString(drugIds));
        } else {
            //通过查询该药企库存，最终确定能否配送
            succFlag = remoteDrugService.scanStock(dbRecipe.getRecipeId(), dep);
            if (!succFlag) {
                LOG.warn("scanStock 药企库存查询返回药品无库存. 处方ID=[{}], 药企ID=[{}], 药企名称=[{}]",
                        dbRecipe.getRecipeId(), dep.getId(), dep.getName());
            }
        }

        return succFlag;
    }

    class SubDepListComparator implements Comparator<DrugsEnterprise> {
        int cp = 0;
        @Override
        public int compare(DrugsEnterprise drugsEnterpriseOne, DrugsEnterprise drugsEnterpriseTwo) {
            int compare = drugsEnterpriseOne.getPayModeSupport() - drugsEnterpriseTwo.getPayModeSupport();
            if (compare != 0) {
                cp = compare > 0 ? 1 : -1;
            }
            return cp;
        }
    }

    private void checkStoreForSendToHom(Recipe dbRecipe, List<DepDetailBean> depDetailList) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> drugsEnterprises = drugsEnterpriseDAO.findByOrganId(dbRecipe.getClinicOrgan());
        for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
            if (DrugEnterpriseConstant.COMPANY_HR.equals(drugsEnterprise.getCallSys())) {
                //将药店配送的药企移除
                BigDecimal recipeFree = BigDecimal.ZERO;
                for (DepDetailBean depDetailBean : depDetailList) {
                    if (depDetailBean.getDepId() == drugsEnterprise.getId()) {
                        recipeFree = depDetailBean.getRecipeFee();
                        depDetailList.remove(depDetailBean);
                        break;
                    }
                }
                //特殊处理,对华润药企特殊处理,包含华润药企,需要将华润药企替换成药店
                RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
                //需要从接口获取药店列表
                DrugEnterpriseResult drugEnterpriseResult = remoteDrugService.findSupportDep(Arrays.asList(dbRecipe.getRecipeId()), null, drugsEnterprise);

                if (DrugEnterpriseResult.SUCCESS.equals(drugEnterpriseResult.getCode())) {
                    Object result = drugEnterpriseResult.getObject();
                    if (result != null && result instanceof List) {
                        List<DepDetailBean> hrList = (List) result;
                        for (DepDetailBean depDetailBean : hrList) {
                            depDetailBean.setDepId(drugsEnterprise.getId());
                            depDetailBean.setBelongDepName(depDetailBean.getDepName());
                            depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_ONLINE);
                            depDetailBean.setPayModeText("在线支付");
                            depDetailBean.setRecipeFee(recipeFree);
                        }
                        depDetailList.addAll(hrList);
                        LOG.info("获取到的药店列表:{}.", JSONUtils.toString(depDetailList));
                    }
                }
            }
        }
    }
}
