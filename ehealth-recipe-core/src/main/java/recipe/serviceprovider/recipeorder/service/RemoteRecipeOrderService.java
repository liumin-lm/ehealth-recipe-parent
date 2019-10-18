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
import recipe.ApplicationUtils;
import recipe.dao.RecipeOrderDAO;
import recipe.service.RecipeOrderService;
import recipe.serviceprovider.BaseService;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
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

}
