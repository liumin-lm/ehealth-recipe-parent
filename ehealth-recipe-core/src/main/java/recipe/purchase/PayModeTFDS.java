package recipe.purchase;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DepListBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.RecipePayModeSupportBean;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.dao.*;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.service.RecipeOrderService;
import recipe.service.RecipeServiceSub;
import recipe.service.common.RecipeCacheService;
import recipe.util.MapValueUtil;
import recipe.util.RedisClient;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 药店取药购药方式
 * @version： 1.0
 */
public class PayModeTFDS implements IPurchaseService{
    private static final Logger LOGGER = LoggerFactory.getLogger(PayModeTFDS.class);
    private RedisClient redisClient = RedisClient.instance();
    private static String EXPIRE_SECOND;

    public PayModeTFDS(){
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);
        EXPIRE_SECOND = cacheService.getRecipeParam("EXPIRE_SECOND", "600");
    }

    @Override
    public RecipeResultBean findSupportDepList(Recipe recipe, Map<String, String> extInfo) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        DepListBean depListBean = new DepListBean();
        Integer recipeId = recipe.getRecipeId();
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

        //获取患者位置信息进行缓存处理
        String range = MapValueUtil.getString(extInfo, "range");
        String longitude = MapValueUtil.getString(extInfo, "longitude");
        String latitude = MapValueUtil.getString(extInfo, "latitude");
        String md5Key = longitude + "-" + latitude + "-" + range;
        String key = recipeId + "-" + DigestUtils.md5DigestAsHex(md5Key.getBytes());

        List<DepDetailBean> depDetailBeans = redisClient.get(key);
        if (CollectionUtils.isNotEmpty(depDetailBeans)) {
            List<DepDetailBean> result = getDepDetailBeansByPage(extInfo, depDetailBeans);
            depListBean.setList(result);
            resultBean.setObject(depListBean);
            return resultBean;
        }
        //获取购药方式查询列表
        List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(getPayMode());
        if (CollectionUtils.isEmpty(payModeSupport)) {
            LOGGER.warn("findSupportDepList 处方[{}]无法匹配配送方式. payMode=[{}]", recipeId, getPayMode());
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("配送模式配置有误");
            return resultBean;
        }
        RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        List<DrugsEnterprise> drugsEnterprises = drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(recipe.getClinicOrgan(), payModeSupport);
        if (CollectionUtils.isEmpty(drugsEnterprises)) {
            //该机构没有对应可药店取药的药企
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("该机构没有配置可药店取药的药企，请到运营平台进行配置");
            return resultBean;
        }
        LOGGER.info("findSupportDepList recipeId={}, 匹配到支持药店取药药企数量[{}]", recipeId, drugsEnterprises.size());
        List<Integer> recipeIds = Arrays.asList(recipeId);
        //处理详情
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        List<Integer> drugIds = new ArrayList<>(detailList.size());
        for (Recipedetail detail : detailList) {
            drugIds.add(detail.getDrugId());
        }
        List<DepDetailBean> depDetailList = new ArrayList<>();

        for (DrugsEnterprise dep : drugsEnterprises) {
            List<DepDetailBean> depList = new ArrayList<>();
            //通过查询该药企对应药店库存
            boolean succFlag = scanStock(recipe, dep, drugIds);
            if (!succFlag) {
                LOGGER.warn("findSupportDepList 当前药企无库存. 药企=[{}], recipeId=[{}]", dep.getName() ,recipeId);
                continue;
            }
            //需要从接口获取药店列表
            DrugEnterpriseResult drugEnterpriseResult = remoteDrugService.findSupportDep(recipeIds, extInfo, dep);
            depList = findAllSupportDeps(drugEnterpriseResult, dep, extInfo);
            depDetailList.addAll(depList);
        }
        //对药店列表进行排序
        String sort = MapValueUtil.getString(extInfo, "sort");
        Collections.sort(depDetailList, new DepDetailBeanComparator(sort));
        if (CollectionUtils.isNotEmpty(depDetailList)) {
            redisClient.setEX(key, Long.parseLong(EXPIRE_SECOND), depDetailList);
        }
        LOGGER.info("findSupportDepList recipeId={}, 获取到药店数量[{}]", recipeId, depDetailList.size());
        List<DepDetailBean> result = getDepDetailBeansByPage(extInfo, depDetailList);
        depListBean.setList(result);
        resultBean.setObject(depListBean);
        return resultBean;
    }

    private List<DepDetailBean> getDepDetailBeansByPage(Map<String, String> extInfo, List<DepDetailBean> depDetailList) {
        Integer start = MapValueUtil.getInteger(extInfo, "start");
        Integer limit = MapValueUtil.getInteger(extInfo, "limit");
        //进行简单分页的操作
        List<DepDetailBean> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(depDetailList) && start != null && depDetailList.size() > start) {
            if (depDetailList.size() < start + limit) {
                result = depDetailList.subList(start, depDetailList.size());
            } else {
                result = depDetailList.subList(start, start + limit);
            }
        }
        return result;
    }

    @Override
    public OrderCreateResult order(Recipe dbRecipe, Map<String, String> extInfo) {
        OrderCreateResult result = new OrderCreateResult(RecipeResultBean.SUCCESS);
        //定义处方订单
        RecipeOrder order = new RecipeOrder();

        //获取当前支持药店的药企
        Integer depId = MapValueUtil.getInteger(extInfo, "depId");
        Integer recipeId = dbRecipe.getRecipeId();
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        DrugsEnterprise dep = drugsEnterpriseDAO.getById(depId);
        //处理详情
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        List<Integer> drugIds = FluentIterable.from(detailList).transform(new Function<Recipedetail, Integer>() {
            @Override
            public Integer apply(Recipedetail input) {
                return input.getDrugId();
            }
        }).toList();
        //患者提交订单前,先进行库存校验

        boolean succFlag = scanStock(dbRecipe, dep, drugIds);
        if(!succFlag){
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("抱歉，配送商库存不足无法配送。请稍后尝试提交，或更换配送商。");
            return result;
        }
        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        RecipePayModeSupportBean payModeSupport = orderService.setPayModeSupport(order, payMode);

        order.setMpiId(dbRecipe.getMpiid());
        order.setOrganId(dbRecipe.getClinicOrgan());
        order.setOrderCode(orderService.getOrderCode(order.getMpiId()));
        order.setStatus(OrderStatusConstant.READY_GET_DRUG);
        order.setDrugStoreCode(MapValueUtil.getString(extInfo, "gysCode"));
        order.setDrugStoreName(MapValueUtil.getString(extInfo, "gysName"));
        order.setRecipeIdList("["+dbRecipe.getRecipeId()+"]");
        order.setDrugStoreAddr(MapValueUtil.getString(extInfo, "gysAddr"));
        order.setEnterpriseId(MapValueUtil.getInteger(extInfo, "depId"));
        order.setDrugStoreCode(MapValueUtil.getString(extInfo, "pharmacyCode"));
        List<Recipe> recipeList = Arrays.asList(dbRecipe);
        Integer calculateFee = MapValueUtil.getInteger(extInfo, "calculateFee");
        if (null == calculateFee || Integer.valueOf(1).equals(calculateFee)) {
            orderService.setOrderFee(result, order, Arrays.asList(recipeId), recipeList, payModeSupport, extInfo, 1);
            if (StringUtils.isNotEmpty(extInfo.get("recipeFee"))) {
                order.setRecipeFee(MapValueUtil.getBigDecimal(extInfo, "recipeFee"));
                order.setActualPrice(Double.parseDouble(extInfo.get("recipeFee")));
            }
        } else {
            //设置默认值
            order.setExpressFee(BigDecimal.ZERO);
            order.setTotalFee(BigDecimal.ZERO);
            order.setRecipeFee(BigDecimal.ZERO);
            order.setCouponFee(BigDecimal.ZERO);
            order.setRegisterFee(BigDecimal.ZERO);
            order.setActualPrice(BigDecimal.ZERO.doubleValue());
        }

        //设置为有效订单
        order.setEffective(1);
        boolean saveFlag = orderService.saveOrderToDB(order, recipeList, payMode, result, recipeDAO, orderDAO);
        if(!saveFlag){
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("提交失败，请重新提交。");
            return result;
        }
        orderService.setCreateOrderResult(result, order, payModeSupport, 1);
        //更新处方信息
        orderService.finishOrderPayWithoutPay(order.getOrderCode(), payMode);
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
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        Integer recipeId = dbRecipe.getRecipeId();
        boolean succFlag;
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
        succFlag = remoteDrugService.scanStock(recipeId, dep);
        if (!succFlag) {
            LOGGER.warn("findSupportDepList 药企库存查询返回药品无库存. 处方ID=[{}], 药企ID=[{}], 药企名称=[{}]",
                    recipeId, dep.getId(), dep.getName());
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

    private List<DepDetailBean> findAllSupportDeps(DrugEnterpriseResult drugEnterpriseResult, DrugsEnterprise dep, Map<String, String> extInfo){
        List<DepDetailBean> depDetailList = new ArrayList<>();
        if (DrugEnterpriseResult.SUCCESS.equals(drugEnterpriseResult.getCode())) {
            Object result = drugEnterpriseResult.getObject();
            if (result != null && result instanceof List) {
                List<DepDetailBean> ysqList = (List) result;
                for (DepDetailBean depDetailBean : ysqList) {
                    depDetailBean.setDepId(dep.getId());
                    depDetailBean.setBelongDepName(dep.getName());
                    depDetailBean.setPayModeText("药店支付");
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
                Double distance = depDetailBeanOne.getDistance() - depDetailBeanTwo.getDistance();
                if (distance != 0.0) {
                    cp = (distance > 0.0) ? 2 : -1;
                }
            }
            return cp;
        }
    }
}
