package recipe.purchase;

import com.google.common.collect.Maps;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DepListBean;
import com.ngari.recipe.entity.*;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.RecipePayModeSupportBean;
import recipe.constant.RecipeBussConstant;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.service.RecipeOrderService;
import recipe.service.RecipeService;
import recipe.service.RecipeServiceSub;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public RecipeResultBean findSupportDepList(Recipe dbRecipe, Map ext) {
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
        if (CollectionUtils.isEmpty(payModeSupport)) {
            LOG.warn("findSupportDepList 处方[{}]无法匹配配送方式. payMode=[{}]", recipeId, getPayMode());
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("配送模式配置有误");
            return resultBean;
        }

        //筛选出来的数据已经去掉不支持任何方式配送的药企
        List<DrugsEnterprise> drugsEnterpriseList =
                drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(dbRecipe.getClinicOrgan(), payModeSupport);
        if (CollectionUtils.isEmpty(drugsEnterpriseList)) {
            LOG.warn("findSupportDepList 处方[{}]没有任何药企可以进行配送！", recipeId);
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("没有药企可以配送");
            return resultBean;
        }

        RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);

        //处理详情
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        List<Integer> drugIds = new ArrayList<>(detailList.size());
        Map<Integer, Double> drugIdCountMap = Maps.newHashMap();
        for(Recipedetail detail : detailList){
            drugIds.add(detail.getDrugId());
            drugIdCountMap.put(detail.getDrugId(), detail.getUseTotalDose());
        }
        
        List<DrugsEnterprise> subDepList = new ArrayList<>(drugsEnterpriseList.size());
        for (DrugsEnterprise dep : drugsEnterpriseList) {
            //根据药企是否能满足所有配送的药品优先
            Integer depId = dep.getId();
            //药品匹配成功标识
            boolean succFlag = false;
            Long count = saleDrugListDAO.getCountByOrganIdAndDrugIds(depId, drugIds);
            if (null != count && count > 0) {
                if (count == drugIds.size()) {
                    succFlag = true;
                }
            }

            if (!succFlag) {
                LOG.warn("findSupportDepList 存在不支持配送药品. 处方ID=[{}], 药企ID=[{}], 药企名称=[{}], drugIds={}",
                        recipeId, depId, dep.getName(), JSONUtils.toString(drugIds));
                continue;
            } else {
                //通过查询该药企库存，最终确定能否配送
                succFlag = remoteDrugService.scanStock(recipeId, dep);
                if (succFlag) {
                    subDepList.add(dep);
                } else {
                    LOG.warn("findSupportDepList 药企库存查询返回药品无库存. 处方ID=[{}], 药企ID=[{}], 药企名称=[{}]",
                            recipeId, depId, dep.getName());
                }
            }
        }

        if (CollectionUtils.isEmpty(subDepList)) {
            LOG.warn("findSupportDepList 该处方无法配送. recipeId=[{}]", recipeId);
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("没有药企可以配送");
            return resultBean;
        }

        DepDetailBean depDetailBean;
        for (DrugsEnterprise dep : subDepList) {
            depDetailBean = new DepDetailBean();
            depDetailBean.setDepId(dep.getId());
            depDetailBean.setDepName(dep.getName());
            depDetailBean.setRecipeFee(dbRecipe.getTotalMoney());
            depDetailBean.setBelongDepName(dep.getName());
            depDetailBean.setPayModeText("在线支付");

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

        depListBean.setSigle(false);
        if(CollectionUtils.isNotEmpty(depDetailList) && depDetailList.size() == 1){
            depListBean.setSigle(true);
        }

        depListBean.setList(depDetailList);
        resultBean.setObject(depListBean);
        return resultBean;
    }

    @Override
    public RecipeResultBean order(Recipe dbRecipe, Map<String, String> extInfo) {
        RecipeOrder order = new RecipeOrder();
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

        Integer recipeId = dbRecipe.getRecipeId();
        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        RecipePayModeSupportBean payModeSupport = orderService.setPayModeSupport(order, payMode);
        Integer depId = MapValueUtil.getInteger(extInfo, "depId");
        Integer fitDepId = recipeService.supportDistributionExt(recipeId, dbRecipe.getClinicOrgan(), depId, payMode);
        /*if(null == fitDepId){
            LOG.warn("order 该处方无法配送. recipeId={}, depId={}", recipeId);
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("没有药企可以配送");
            return resultBean;
        }
        //设置订单各种费用和配送地址
        Integer calculateFee = MapValueUtil.getInteger(extInfo, "calculateFee");
        if (null == calculateFee || Integer.valueOf(1).equals(calculateFee)) {
            setOrderFee(result, order, recipeIds, recipeList, payModeSupport, extInfo, toDbFlag);
        } else {
            order.setRecipeFee(BigDecimal.ZERO);
            order.setCouponFee(BigDecimal.ZERO);
            order.setRegisterFee(BigDecimal.ZERO);
            order.setExpressFee(BigDecimal.ZERO);
            order.setTotalFee(BigDecimal.ZERO);
            order.setActualPrice(BigDecimal.ZERO.doubleValue());
        }*/
        


        return null;
    }

    @Override
    public Integer getPayMode() {
        return RecipeBussConstant.PAYMODE_ONLINE;
    }

    @Override
    public String getServiceName() {
        return "payModeOnlineService";
    }
}
