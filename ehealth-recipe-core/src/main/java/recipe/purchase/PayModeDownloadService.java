package recipe.purchase;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.RecipePayModeSupportBean;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.service.RecipeOrderService;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
* @Description: PayModeDownloadService 类（或接口）是 承接购药方式中下载处方方式
* @Author: JRK
* @Date: 2019/8/6
*/
public class PayModeDownloadService implements IPurchaseService{

    @Override
    public RecipeResultBean findSupportDepList(Recipe dbRecipe, Map<String, String> extInfo) {
        //现在是三种返回，1.object放入页面转入的药店列表和供应商列表； 2.msg存的是弹出固定弹窗的信息
        //提示信息； 3。ext中存入特应性弹窗的识别特性
        //页面的弹窗信息根据处方信息中，根据配置的审核方式的不同谈处方不同的弹窗
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Map<String, String> ext = new HashedMap();
        //当不需要审核的时候弹出特性弹窗
        if(ReviewTypeConstant.Not_Need_Check == dbRecipe.getReviewType()){
            ext.put("popupFeature", "noCheck");
        }else{
            ext.put("popupFeature", "needCheck");
        }
        resultBean.setExt(ext);
        return resultBean;
    }

    @Override
    public OrderCreateResult order(Recipe dbRecipe, Map<String, String> extInfo) {
        //PayAmountTypeStatus payAmountTypeStatus = BuyMedicineProcessHandle.changeConfigToPayAmount(MapValueUtil.getInteger(extInfo, "reviewType"), MapValueUtil.getString(extInfo, "payMethod"), Boolean.parseBoolean(extInfo.get("payMethod")));

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
        } else {
            order.setWxPayWay(payway);
        }

        //处理详情
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        List<Integer> drugIds = FluentIterable.from(detailList).transform(new Function<Recipedetail, Integer>() {
            @Override
            public Integer apply(Recipedetail input) {
                return input.getDrugId();
            }
        }).toList();

        order.setEnterpriseId(depId);

        order.setRecipeIdList(JSONUtils.toString(Arrays.asList(recipeId)));

        // 暂时还是设置成处方单的患者，不然用户历史处方列表不好查找
        order.setMpiId(dbRecipe.getMpiid());
        order.setOrganId(dbRecipe.getClinicOrgan());
        order.setOrderCode(orderService.getOrderCode(order.getMpiId()));
        order.setStatus(OrderStatusConstant.READY_GET_DRUG);

        //设置订单各种费用
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
        //orderService.setOrderFee(result, order, Arrays.asList(recipeId), recipeList, payModeSupport, extInfo, 1);

        //设置为有效订单
        order.setEffective(1);
        boolean saveFlag = orderService.saveOrderToDB(order, recipeList, payMode, result, recipeDAO, orderDAO);
        if (!saveFlag) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("订单保存出错");
            return result;
        }

        orderService.setCreateOrderResult(result, order, payModeSupport, 1);

        //修改订单线下支付的状态
        orderService.finishOrderPayWithoutPay(order.getOrderCode(), payMode);
        return result;
    }

    @Override
    public Integer getPayMode() {
        //标识下载处方的支付场景
        return RecipeBussConstant.PAYMODE_DOWNLOAD_RECIPE;
    }

    @Override
    public String getServiceName() {
        //在购药方式上添加下载处方的实现类
        return "payModeDownloadService";
    }

    @Override
    public String getTipsByStatusForPatient(Recipe recipe, RecipeOrder order) {
        Integer status = recipe.getStatus();
        String orderCode = recipe.getOrderCode();
        int orderStatus = null == order ? -1 : order.getStatus();
        String tips = "";
        //下载处方购药方式特殊状态,已下载
        if(OrderStatusConstant.DOWNLOADED == orderStatus || RecipeStatusConstant.PATIENT_NO_PAY == status){
            tips = "已下载处方签";
        }
        //下载处方购药下通用状态的文案
        if(OrderStatusConstant.FINISH == orderStatus || RecipeStatusConstant.FINISH == status){
            tips = "订单完成";
        }
        if(OrderStatusConstant.READY_GET_DRUG == orderStatus){
            tips = "订单已处理，请下载处方笺";
        }
        return tips;
    }
}