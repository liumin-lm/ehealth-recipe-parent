package recipe.atop.greenroom;

import com.ngari.recipe.recipe.model.RecipeOrderWaybillDTO;
import com.ngari.recipe.vo.CodeEnum;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.patient.IRecipeOrderBusinessService;
import recipe.vo.ResultBean;
import recipe.vo.greenroom.DrugUsageLabelResp;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Description
 * @Author yzl
 * @Date 2022-06-02
 */
@RpcBean(value = "recipeGmAtop")
public class RecipeGmAtop extends BaseAtop {

    @Autowired
    IRecipeBusinessService recipeBusinessService;

    @Autowired
    private IRecipeOrderBusinessService recipeOrderService;


    /**
     * 运营平台查询处方单用法标签
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public DrugUsageLabelResp queryRecipeDrugUsageLabel(Integer recipeId) {
        validateAtop(recipeId);
        return recipeBusinessService.queryRecipeDrugUsageLabel(recipeId);
    }

    /**
     * 运营平台查询处方单用法标签
     *
     * @param orderId
     * @return
     */
    @RpcService
    public List<DrugUsageLabelResp> queryRecipeDrugUsageLabelByOrder(Integer orderId) {
        validateAtop(orderId);
        return recipeBusinessService.queryRecipeDrugUsageLabelByOrder(orderId);
    }

    /**
     * 订单状态更新
     */
    @RpcService
    public ResultBean updateRecipeOrderStatusAndGiveUser(UpdateOrderStatusVO updateOrderStatusVO) {
        validateAtop(updateOrderStatusVO,updateOrderStatusVO.getRecipeId());
        ResultBean resultBean1 = new ResultBean();
        if (Objects.nonNull(updateOrderStatusVO.getTargetRecipeOrderStatus())) {
            // 更改状态
            ResultBean resultBean = recipeOrderService.updateRecipeOrderStatus(updateOrderStatusVO);
            if(CodeEnum.SERVICE_ERROR.getCode().equals(resultBean.getCode())){
                resultBean1.setCode(resultBean.getCode());
            }
        }
        if (Objects.nonNull(updateOrderStatusVO.getGiveUser())) {
            // 更改发药人
            ResultBean resultBean = recipeOrderService.updateOrderGiveUser(updateOrderStatusVO.getOrderId(), updateOrderStatusVO.getGiveUser());
            if(CodeEnum.SERVICE_ERROR.getCode().equals(resultBean.getCode())){
                resultBean1.setCode(resultBean.getCode());
            }
        }
        if(Objects.nonNull(updateOrderStatusVO.getLogisticsCompany()) && StringUtils.isNotEmpty(updateOrderStatusVO.getTrackingNumber())){
            // 更改物流信息
            recipeOrderService.updateTrackingNumberByOrderId(updateOrderStatusVO);
        }
        return resultBean1;

    }


    /**
     * 获取当前订单用户下历史订单的运单信息
     * @param mpiId
     * @return
     */
    @RpcService
    public List<RecipeOrderWaybillDTO> findOrderByMpiId(String mpiId) {
        validateAtop(mpiId);
        return recipeOrderService.findOrderByMpiId(mpiId);
    }

    @RpcService
    public List<Map<String, Object>> findRecipeListByOrderCode(String orderCode) {
        validateAtop(orderCode);
        return recipeBusinessService.findRecipeDetailsByOrderCode(orderCode);
    }

}
