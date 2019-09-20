package recipe.purchase;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.persistence.DAOFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import recipe.ApplicationUtils;
import recipe.bean.RecipePayModeSupportBean;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.service.RecipeOrderService;
import recipe.util.MapValueUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/20
 * @description： 到院取药方式实现
 * @version： 1.0
 */
public class PayModeToHos implements IPurchaseService{

    @Override
    public RecipeResultBean findSupportDepList(Recipe dbRecipe, Map<String, String> extInfo) {
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Integer recipeId = dbRecipe.getRecipeId();
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
        StringBuilder sb = new StringBuilder();

        if(CollectionUtils.isNotEmpty(detailList)){
            String pharmNo = detailList.get(0).getPharmNo();
            if(StringUtils.isNotEmpty(pharmNo)){
                sb.append("选择到院自取后需去医院取药窗口取药：["+ organDTO.getName() + pharmNo + "取药窗口]");
            }else {
                sb.append("选择到院自取后，需去医院取药窗口取药");
            }
        }
        resultBean.setMsg(sb.toString());
        return resultBean;
    }

    @Override
    public OrderCreateResult order(Recipe dbRecipe, Map<String, String> extInfo) {
        OrderCreateResult result = new OrderCreateResult(RecipeResultBean.SUCCESS);
        //定义处方订单
        RecipeOrder order = new RecipeOrder();
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);

        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        RecipePayModeSupportBean payModeSupport = orderService.setPayModeSupport(order, payMode);

        order.setMpiId(dbRecipe.getMpiid());
        order.setOrganId(dbRecipe.getClinicOrgan());
        order.setOrderCode(orderService.getOrderCode(order.getMpiId()));
        //订单的状态统一到finishOrderPayWithoutPay中设置
        //order.setStatus(OrderStatusConstant.READY_GET_DRUG);
        order.setRecipeIdList("["+dbRecipe.getRecipeId()+"]");
        List<Recipe> recipeList = Arrays.asList(dbRecipe);
        Integer calculateFee = MapValueUtil.getInteger(extInfo, "calculateFee");
        CommonOrder.createDefaultOrder(extInfo, result, order, payModeSupport, recipeList, calculateFee);
        //设置为有效订单
        order.setEffective(1);
        boolean saveFlag = orderService.saveOrderToDB(order, recipeList, payMode, result, recipeDAO, orderDAO);
        if(!saveFlag){
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("提交失败，请重新提交。");
            return result;
        }
        orderService.setCreateOrderResult(result, order, payModeSupport, 1);
        return result;
    }

    @Override
    public Integer getPayMode() {
        return RecipeBussConstant.PAYMODE_TO_HOS;
    }

    @Override
    public String getServiceName() {
        return "payModeToHosService";
    }

    @Override
    public String getTipsByStatusForPatient(Recipe recipe, RecipeOrder order) {
        Integer status = recipe.getStatus();
        Integer payFlag = recipe.getPayFlag();
        String orderCode = recipe.getOrderCode();
        String tips = "";
        switch (status) {
            case RecipeStatusConstant.CHECK_PASS:
                if (StringUtils.isEmpty(orderCode) && payFlag == 1) {
                    tips = "订单已处理，请到院取药";
                }
                break;
            case RecipeStatusConstant.CHECK_PASS_YS:
                tips = "处方已审核通过，请到院取药";
                break;
            case RecipeStatusConstant.RECIPE_FAIL:
                tips = "到院取药失败";
                break;
            case RecipeStatusConstant.FINISH:
                tips = "到院取药成功，订单完成";
                break;
        }
        return tips;
    }
}
