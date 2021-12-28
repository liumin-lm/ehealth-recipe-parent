package recipe.purchase;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DepListBean;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.RecipePayModeSupportBean;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.core.api.IStockBusinessService;
import recipe.dao.*;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.manager.EnterpriseManager;
import recipe.manager.OrderManager;
import recipe.service.RecipeOrderService;
import recipe.service.RecipeServiceSub;
import recipe.service.common.RecipeCacheService;
import recipe.util.MapValueUtil;
import recipe.util.RedisClient;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 药店取药购药方式
 * @version： 1.0
 */
public class PayModeTFDS implements IPurchaseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayModeTFDS.class);
    private RedisClient redisClient = RedisClient.instance();
    private static String EXPIRE_SECOND;
    @Autowired
    private IStockBusinessService stockBusinessService;
    @Autowired
    private OrderManager orderManager;
    @Autowired
    private EnterpriseManager enterpriseManager;

    public PayModeTFDS() {
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);
        EXPIRE_SECOND = cacheService.getRecipeParam("EXPIRE_SECOND", "600");
    }

    @Override
    public RecipeResultBean findSupportDepList(Recipe recipe, Map<String, String> extInfo) {
        LOGGER.info("PayModeTFDS findSupportDepList recipe:{}, extInfo:{}.", JSONUtils.toString(recipe), JSONUtils.toString(extInfo));
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        DepListBean depListBean = new DepListBean();
        Integer recipeId = recipe.getRecipeId();
        RecipeDetailDAO detailDAO = getDAO(RecipeDetailDAO.class);

        //获取患者位置信息进行缓存处理
        String range = MapValueUtil.getString(extInfo, "range");
        String sort = MapValueUtil.getString(extInfo, "sort");
        if (StringUtils.isEmpty(range)) {
            range = "10";
        }
        //获取购药方式查询列表
        List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(getPayMode());
        // 获取药企
        List<DrugsEnterprise> drugsEnterprises = enterpriseManager.findEnterpriseByTFDS(recipe,payModeSupport);
        if (CollectionUtils.isEmpty(drugsEnterprises)) {
            //该机构没有对应可药店取药的药企
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("该机构没有配置可药店取药的药企，请到运营平台进行配置");
            return resultBean;
        }
        LOGGER.info("findSupportDepList recipeId={}, 匹配到支持药店取药药企数量[{}]", recipeId, drugsEnterprises.size());
        List<Integer> recipeIds = Arrays.asList(recipeId);
        //处理详情
        List<Integer> recipeIdList = Arrays.asList(recipeId);
        //合并处方药品费用处理
        String recipeIdsforMerge = MapValueUtil.getString(extInfo, "recipeIds");
        if (StringUtils.isNotEmpty(recipeIdsforMerge)) {
            List<String> recipeIdString = Splitter.on(",").splitToList(recipeIdsforMerge);
            recipeIdList = recipeIdString.stream().map(Integer::valueOf).collect(Collectors.toList());
        }
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        List<Integer> drugIds = new ArrayList<>(detailList.size());
        for (Recipedetail detail : detailList) {
            drugIds.add(detail.getDrugId());
        }

        List<DepDetailBean> depDetailList = new ArrayList<>();
        //针对浙江省互联网药店取药走的是配送模式的处理
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
            DrugsEnterprise drugsEnterprise = drugsEnterprises.get(0);
            if (new Integer(0).equals(drugsEnterprise.getOrderType())){
                //表示跳转第三方订单,给前端返回具体的药企
                DepDetailBean depDetailBean = new DepDetailBean();
                depDetailBean.setDepName(drugsEnterprise.getName());
                depDetailBean.setOrderType(drugsEnterprise.getOrderType());
                depDetailList.add(depDetailBean);
                depListBean.setList(depDetailList);
                depListBean.setSigle(true);
                resultBean.setObject(depListBean);
                return resultBean;
            }
        }
        RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        for (DrugsEnterprise dep : drugsEnterprises) {
            //通过查询该药企对应药店库存
            EnterpriseStock enterpriseStock = stockBusinessService.enterpriseStockCheck(recipe, detailList, dep.getId());
            if ((null != enterpriseStock && enterpriseStock.getStock()) || dep.getCheckInventoryFlag().equals(2)) {
                //需要从接口获取药店列表
                DrugEnterpriseResult drugEnterpriseResult = remoteDrugService.findSupportDep(recipeIds, extInfo, dep);
                List<DepDetailBean> depList = findAllSupportDeps(drugEnterpriseResult, dep, extInfo);
                depDetailList.addAll(depList);
            }
        }
        if (CollectionUtils.isNotEmpty(depDetailList)) {
            Iterator iterator = depDetailList.iterator();
            while (iterator.hasNext()) {
                DepDetailBean depDetailBean = (DepDetailBean)iterator.next();
                Integer depId = depDetailBean.getDepId();
                //如果是价格自定义的药企，则需要设置单独价格
                RecipeOrderService recipeOrderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                //重置药企处方价格
                depDetailBean.setRecipeFee(recipeOrderService.reCalculateRecipeFee(depId, recipeIdList, null));
                //设置距离的小数精度，保留小数点一位
                if (depDetailBean.getDistance() != null) {
                    BigDecimal bd = new BigDecimal(depDetailBean.getDistance());
                    bd = bd.setScale(1, BigDecimal.ROUND_HALF_UP);
                    depDetailBean.setDistance(bd.doubleValue());
                    if (Double.parseDouble(range) < bd.doubleValue()) {
                        iterator.remove();
                    }
                }
            }
        }

        //对药店列表进行排序
        Collections.sort(depDetailList, new DepDetailBeanComparator(sort));
        LOGGER.info("findSupportDepList recipeId={}, 获取到药店数量[{}]", recipeId, depDetailList.size());
        depListBean.setList(depDetailList);
        resultBean.setObject(depListBean);
        LOGGER.info("findSupportDepList recipeId:{},resultBean:{}.", recipeId, JSONUtils.toString(resultBean));
        return resultBean;
    }

    @Override
    public OrderCreateResult order(List<Recipe> dbRecipes, Map<String, String> extInfo) {
        OrderCreateResult result = new OrderCreateResult(RecipeResultBean.SUCCESS);
        //定义处方订单
        RecipeOrder order = new RecipeOrder();

        List<Integer> recipeIdLists = dbRecipes.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        //获取当前支持药店的药企
        Integer depId = MapValueUtil.getInteger(extInfo, "depId");
        DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
        RecipeDetailDAO detailDAO = getDAO(RecipeDetailDAO.class);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        DrugsEnterprise dep = drugsEnterpriseDAO.getById(depId);
        for (Recipe dbRecipe : dbRecipes) {
            //处理详情
            List<Recipedetail> detailList = detailDAO.findByRecipeId(dbRecipe.getRecipeId());
            //患者提交订单前,先进行库存校验
            // 根据药企查询库存
            EnterpriseStock enterpriseStock = stockBusinessService.enterpriseStockCheck(dbRecipe, detailList, depId);
            if ((Objects.isNull(enterpriseStock) || !enterpriseStock.getStock()) && dep.getCheckInventoryFlag() != 2) {
                result.setCode(RecipeResultBean.FAIL);
                result.setMsg("抱歉，配送商库存不足无法配送。请稍后尝试提交，或更换配送商。");
                return result;
            }
        }

        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        RecipePayModeSupportBean payModeSupport = orderService.setPayModeSupport(order, payMode);
        //订单类型-1省医保
        Integer orderType = MapValueUtil.getInteger(extInfo, "orderType");
        order.setMpiId(dbRecipes.get(0).getMpiid());
        order.setOrganId(dbRecipes.get(0).getClinicOrgan());
        order.setOrderCode(orderService.getOrderCode(order.getMpiId()));
        //订单的状态统一到finishOrderPayWithoutPay中设置
        order.setStatus(OrderStatusConstant.HAS_DRUG);
        order.setDrugStoreName(MapValueUtil.getString(extInfo, "gysName"));
        order.setDrugStoreAddr(MapValueUtil.getString(extInfo, "gysAddr"));
        order.setEnterpriseId(MapValueUtil.getInteger(extInfo, "depId"));
        order.setDrugStoreCode(MapValueUtil.getString(extInfo, "pharmacyCode"));

        order.setRecipeIdList(JSONUtils.toString(recipeIdLists));

        Integer calculateFee = MapValueUtil.getInteger(extInfo, "calculateFee");
        //设置中药代建费
        Integer decoctionId = MapValueUtil.getInteger(extInfo, "decoctionId");
        RecipeExtendDAO recipeExtendDAO = getDAO(RecipeExtendDAO.class);
        if(decoctionId != null){
            DrugDecoctionWayDao drugDecoctionWayDao = getDAO(DrugDecoctionWayDao.class);
            DecoctionWay decoctionWay = drugDecoctionWayDao.get(decoctionId);
            for (Recipe dbRecipe : dbRecipes) {
                if (decoctionWay != null) {
                    if (decoctionWay.getDecoctionPrice() != null) {
                        calculateFee = 1;
                        order.setDecoctionUnitPrice(BigDecimal.valueOf(decoctionWay.getDecoctionPrice()));
                    }
                    recipeExtendDAO.updateRecipeExInfoByRecipeId(dbRecipe.getRecipeId(), ImmutableMap.of("decoctionId", decoctionId + "", "decoctionText", decoctionWay.getDecoctionText()));
                } else {
                    LOGGER.error("未获取到对应的代煎费，recipeId={},decoctionId={}", dbRecipe.getRecipeId(), decoctionId);
                }
            }
        }
        // 目前paymode传入还是老版本 除线上支付外全都算线下支付,下个版本与前端配合修改
        Integer payModeNew = payMode;
        if(!payMode.equals(1)){
            payModeNew = 2;
        }
        //如果是医保支付前端目前传的orderType都是1,杭州市医保得特殊处理
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(dbRecipes.get(0).getRecipeMode())
                && RecipeBussConstant.ORDERTYPE_ZJS.equals(orderType)) {
            orderType = RecipeBussConstant.ORDERTYPE_HZS;
            LOGGER.info("getOrderCreateResult.orderType ={}", orderType);
            order.setOrderType(orderType);
            if (dep.getStorePayFlag() == 1) {
                payModeNew = RecipeBussConstant.PAYMODE_ONLINE;
            }
        }
        CommonOrder.createDefaultOrder(extInfo, result, order, payModeSupport, dbRecipes, calculateFee);
        //设置为有效订单
        order.setEffective(1);

        order.setPayMode(payModeNew);
        boolean saveFlag = orderService.saveOrderToDB(order, dbRecipes, payMode, result, recipeDAO, orderDAO);
        if(!saveFlag){
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("提交失败，请重新提交。");
            return result;
        }
        orderService.setCreateOrderResult(result, order, payModeSupport, 1);
        //更新处方信息
        if(0d >= order.getActualPrice()){
            //如果不需要支付则不走支付,直接掉支付后的逻辑
            orderService.finishOrderPay(order.getOrderCode(), 1, MapValueUtil.getInteger(extInfo, "payMode"));
        }else{
            // 邵逸夫模式下 不需要审方物流费需要生成一条流水记录
            orderManager.saveFlowByOrder(order);

            //需要支付则走支付前的逻辑
            orderService.finishOrderPayWithoutPay(order.getOrderCode(), payMode);
        }
        //根据药企判断是否药店取药改药品价格
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        if(drugsEnterprise != null && drugsEnterprise.getStorePayFlag() != null && drugsEnterprise.getStorePayFlag() == 1){
            for (Integer reicpeId : recipeIdLists) {
                PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
                purchaseService.updateRecipeDetail(reicpeId);
            }
        }
        for (Integer recipeId2 : recipeIdLists){
            PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
            purchaseService.updateRecipeDetail(recipeId2);
            //date 20200318
            //确认订单后同步配送信息接口
            extInfo.put("payMode", "4");
            extInfo.put("drugStoreCode", order.getDrugStoreCode());
            extInfo.put("drugStoreName", order.getDrugStoreName());
            CommonOrder.updateGoodsReceivingInfoToCreateOrder(recipeId2,extInfo);
        }
        return result;
    }

    /**
     * 判断药企药店库存，包含平台内权限及药企药店实时库存
     * @param dbRecipe  处方单
     * @param dep       药企
     * @param drugIds   药品列表
     * @return          是否存在库存
     */
    private boolean scanStock(Recipe dbRecipe, DrugsEnterprise dep, List<Integer> drugIds) {
        SaleDrugListDAO saleDrugListDAO = getDAO(SaleDrugListDAO.class);
        RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        Integer recipeId = dbRecipe.getRecipeId();
        boolean succFlag = false;
        if(null == dep || CollectionUtils.isEmpty(drugIds)){
            return false;
        }
        //判断药企平台内药品权限，此处简单判断数量是否一致
        Long count = saleDrugListDAO.getCountByOrganIdAndDrugIds(dep.getId(), drugIds);
        if (null != count && count > 0) {
            if (count == drugIds.size()) {
                succFlag = true;
            }
        }

        DrugEnterpriseResult result = remoteDrugService.scanStock(recipeId, dep);
        succFlag = result.getCode().equals(DrugEnterpriseResult.SUCCESS) ? true : false;
        if (!succFlag) {
            LOGGER.warn("findSupportDepList 药企库存查询返回药品无库存. 处方ID=[{}], 药企ID=[{}], 药企名称=[{}]", recipeId, dep.getId(), dep.getName());
        }
        return succFlag;
    }

    @Override
    public Integer getPayMode() {
        return RecipeBussConstant.PAYMODE_TFDS;
    }

    @Override
    public String getServiceName() {
        return "payModeTFDSService";
    }

    @Override
    public String getTipsByStatusForPatient(Recipe recipe, RecipeOrder order) {
        Integer status = recipe.getStatus();
        String orderCode = recipe.getOrderCode();
        String tips = "";
        int orderStatus;
        switch (RecipeStatusEnum.getRecipeStatusEnum(status)) {
            case RECIPE_STATUS_CHECK_PASS:
                if (StringUtils.isEmpty(orderCode)) {
                    break;
                }
                orderStatus = order.getStatus();
                if (orderStatus == RecipeOrderStatusEnum.ORDER_STATUS_HAS_DRUG.getType()) {
                    tips = "订单已处理，请到店取药";
                } else if (orderStatus == RecipeOrderStatusEnum.ORDER_STATUS_READY_DRUG.getType()) {
                    tips = "订单已处理，正在准备药品";
                } else if (orderStatus == RecipeOrderStatusEnum.ORDER_STATUS_NO_DRUG.getType()) {
                    tips = "药品已准备好，请到药店取药";
                }
                break;
            case RECIPE_STATUS_CHECK_PASS_YS:
                if (StringUtils.isEmpty(orderCode)) {
                    break;
                }
                orderStatus = order.getStatus();
                if (orderStatus == RecipeOrderStatusEnum.ORDER_STATUS_HAS_DRUG.getType()) {
                    tips = "处方已审核通过，请到店取药";
                } else if (orderStatus == RecipeOrderStatusEnum.ORDER_STATUS_READY_DRUG.getType()) {
                    tips = "处方已审核通过，正在准备药品";
                } else if (orderStatus == RecipeOrderStatusEnum.ORDER_STATUS_NO_DRUG.getType()) {
                    tips = "药品已准备好，请到药店取药";
                }
                break;
            case RECIPE_STATUS_HAVE_PAY:
                tips = "订单已处理，请到店取药";
                break;
            case RECIPE_STATUS_RECIPE_FAIL:
                tips = "药店取药失败";
                break;
            case RECIPE_STATUS_FINISH:
                tips = "到店取药成功，订单完成";
                break;
                default:
        }
        return tips;
    }

    @Override
    public Integer getOrderStatus(Recipe recipe) {
        RecipeDetailDAO detailDAO = getDAO(RecipeDetailDAO.class);
        DrugsEnterpriseDAO enterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise dep = enterpriseDAO.getById(recipe.getEnterpriseId());
        //处理详情
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipe.getRecipeId());
        List<Integer> drugIds = new ArrayList<>(detailList.size());
        for (Recipedetail detail : detailList) {
            drugIds.add(detail.getDrugId());
        }
        boolean succFlag = scanStock(recipe, dep, drugIds);
        Integer orderStatus = RecipeOrderStatusEnum.ORDER_STATUS_HAS_DRUG.getType();
        if (!succFlag && dep.getCheckInventoryFlag() == 2) {
            orderStatus = RecipeOrderStatusEnum.ORDER_STATUS_READY_DRUG.getType();
        }
        return orderStatus;
    }

    @Override
    public void setRecipePayWay(RecipeOrder recipeOrder) {
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        DrugsEnterpriseDAO enterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = enterpriseDAO.getById(recipeOrder.getEnterpriseId());
        if (new Integer(1).equals(drugsEnterprise.getStorePayFlag())) {
            //药品费用线上支付
            recipeOrder.setPayMode(1);
        } else {
            recipeOrder.setPayMode(2);
        }
        recipeOrderDAO.update(recipeOrder);
    }

    private List<DepDetailBean> findAllSupportDeps(DrugEnterpriseResult drugEnterpriseResult, DrugsEnterprise dep, Map<String, String> extInfo){
        List<DepDetailBean> depDetailList = new ArrayList<>();
        if (null != drugEnterpriseResult && DrugEnterpriseResult.SUCCESS.equals(drugEnterpriseResult.getCode())) {
            Object result = drugEnterpriseResult.getObject();
            if (result != null && result instanceof List) {
                List<DepDetailBean> ysqList = (List) result;
                for (DepDetailBean depDetailBean : ysqList) {
                    depDetailBean.setDepId(dep.getId());
                    depDetailBean.setBelongDepName(dep.getName());
                    depDetailBean.setPayModeText("药店支付");
                    depDetailBean.setOrderType(dep.getOrderType());
                }
                depDetailList.addAll(ysqList);
                LOGGER.info("获取到的药店列表:{}.", JSONUtils.toString(depDetailList));
            }
        }
        return depDetailList;
    }

    class DepDetailBeanComparator implements Comparator<DepDetailBean> {
        String sort;
        DepDetailBeanComparator(String sort){
            this.sort = sort;
        }
        @Override
        public int compare(DepDetailBean depDetailBeanOne, DepDetailBean depDetailBeanTwo) {
            int cp = 0;
            if (StringUtils.isNotEmpty(sort) && sort.equals("1")) {
                //价格排序
                BigDecimal price = depDetailBeanOne.getRecipeFee().subtract(depDetailBeanTwo.getRecipeFee());
                int compare = price.compareTo(BigDecimal.ZERO);
                if (compare != 0) {
                    cp = (compare > 0) ? 2 : -1;
                }
            } else {
                //距离排序
                if (depDetailBeanOne.getDistance() == null || depDetailBeanTwo.getDistance() == null) {
                    return -1;
                }
                Double distance = depDetailBeanOne.getDistance() - depDetailBeanTwo.getDistance();
                if (distance != 0.0) {
                    cp = (distance > 0.0) ? 2 : -1;
                }
            }
            return cp;
        }
    }
}
