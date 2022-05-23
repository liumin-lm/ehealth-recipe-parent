package recipe.atop.open;

import com.alibaba.fastjson.JSONArray;
import com.ngari.common.dto.CheckRequestCommonOrderPageDTO;
import com.ngari.common.dto.SyncOrderVO;
import com.ngari.platform.recipe.mode.RecipeBean;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.dto.RecipeOrderDto;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.util.annotation.RpcBean;
import eh.utils.BeanCopyUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.api.open.IRecipeOrderAtopService;
import recipe.atop.BaseAtop;
import recipe.core.api.patient.IRecipeOrderBusinessService;
import recipe.vo.second.RecipeOrderVO;
import recipe.vo.second.RecipeVo;
import recipe.vo.second.enterpriseOrder.DownOrderRequestVO;
import recipe.vo.second.enterpriseOrder.EnterpriseDownDataVO;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @description： 处方订单 open 实现类
 * @author： whf
 * @date： 2021-11-08 15:46
 */
@RpcBean("recipeOrderOpenAtop")
public class RecipeOrderOpenAtop extends BaseAtop implements IRecipeOrderAtopService {
    @Autowired
    private IRecipeOrderBusinessService recipeOrderService;

    @Override
    public RecipeOrderVO getRecipeOrderByBusId(Integer orderId) {
        logger.info("RecipeOrderOpenAtop getRecipeOrderByBusId req orderId={}", orderId);
        validateAtop(orderId);
        RecipeOrderDto recipeOrderDto = recipeOrderService.getRecipeOrderByBusId(orderId);
        if(Objects.isNull(recipeOrderDto)){
            return null;
        }
        RecipeOrderVO recipeOrderVO = new RecipeOrderVO();
        BeanUtils.copyProperties(recipeOrderDto,recipeOrderVO);
        List<RecipeVo> collect = recipeOrderDto.getRecipeList().stream().map(recipeBeanDTO -> {
            RecipeVo recipeVo = new RecipeVo();
            BeanCopyUtils.copy(recipeBeanDTO,recipeVo);
            return recipeVo;
        }).collect(Collectors.toList());
        recipeOrderVO.setRecipeVos(collect);
        logger.info("RecipeOrderOpenAtop getRecipeOrderByBusId res  recipeOrderVO={}", JSONArray.toJSONString(recipeOrderVO));
        return recipeOrderVO;
    }

    @Override
    public CheckRequestCommonOrderPageDTO getRecipePageForCommonOrder(SyncOrderVO request) {
        logger.info("RecipeOrderOpenAtop getRevisitPageForCommonOrder req request={}", JSONArray.toJSONString(request));
        CheckRequestCommonOrderPageDTO checkRequestCommonOrderPageDTO = new CheckRequestCommonOrderPageDTO();
        if (request.getPage() == null || request.getSize() == null) {
            return checkRequestCommonOrderPageDTO;
        }
        checkRequestCommonOrderPageDTO = recipeOrderService.getRecipePageForCommonOrder(request);
        logger.info("RecipeOrderOpenAtop getRevisitPageForCommonOrder res CheckRequestCommonOrderPageDTO={}", JSONArray.toJSONString(checkRequestCommonOrderPageDTO));
        return checkRequestCommonOrderPageDTO;
    }

    @Override
    public Boolean updateTrackingNumberByOrderCode(String orderCode, String trackingNumber) {
        validateAtop(orderCode, trackingNumber);
        return recipeOrderService.updateTrackingNumberByOrderCode(orderCode, trackingNumber);
    }

    @Override
    public EnterpriseDownDataVO findOrderAndRecipes(DownOrderRequestVO downOrderRequestVO) {
        validateAtop(downOrderRequestVO, downOrderRequestVO.getAppKey());
        validateAtop(downOrderRequestVO.getBeginTime(), downOrderRequestVO.getEndTime());
        return recipeOrderService.findOrderAndRecipes(downOrderRequestVO);
    }

    @Override
    public RecipeResultBean cancelOrderByRecipeId(Integer recipeId, Integer status) {
        return recipeOrderService.cancelOrderByRecipeId(recipeId, status);
    }

    @Override
    public String getTrackingNumber(RecipeBean recipeBean) {
        validateAtop(recipeBean, recipeBean.getClinicOrgan(), recipeBean.getRecipeCode());
        RecipeOrder recipeOrder = recipeOrderService.getTrackingNumber(recipeBean.getRecipeCode(), recipeBean.getClinicOrgan());
        if (null == recipeOrder) {
            return null;
        }
        return recipeOrder.getTrackingNumber();
    }
}
