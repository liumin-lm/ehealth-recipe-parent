package recipe.api.open;

import com.ngari.common.dto.CheckRequestCommonOrderPageDTO;
import com.ngari.common.dto.SyncOrderVO;
import com.ngari.platform.recipe.mode.RecipeBean;
import com.ngari.platform.recipe.mode.RecipeOrderBean;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.recipe.model.*;
import ctd.util.annotation.RpcService;
import recipe.vo.second.CabinetVO;
import recipe.vo.second.RecipeOrderVO;
import recipe.vo.second.enterpriseOrder.DownOrderRequestVO;
import recipe.vo.second.enterpriseOrder.EnterpriseDownDataVO;

import java.util.List;

/**
 * @description： 处方订单第三方
 * @author： whf
 * @date： 2021-11-08 15:35
 */
public interface IRecipeOrderAtopService {

    /**
     * 根据订单id 查询订单信息
     * @param orderId
     * @return
     */
    @RpcService(mvcDisabled = true)
    RecipeOrderVO getRecipeOrderByBusId(Integer orderId);

    /**
     *  端历史数据同步使用
     * @param request
     * @return
     */
    @RpcService(mvcDisabled = true)
    CheckRequestCommonOrderPageDTO getRecipePageForCommonOrder(SyncOrderVO request);

    /**
     *  根据订单号更新物流单号
     * @param orderCode 订单号
     * @param trackingNumber 物流单号
     * @return 是否更新成功
     */
    @RpcService(mvcDisabled = true)
    Boolean updateTrackingNumberByOrderCode(String orderCode, String trackingNumber);

    /**
     * 第三方查询平台处方订单信息
     * @param downOrderRequestVO 请求入参
     * @return 处方订单列表
     */
    @RpcService
    EnterpriseDownDataVO findOrderAndRecipes(DownOrderRequestVO downOrderRequestVO);

    /**
     * 根据处方号取消订单
     *
     * @param recipeId 处方号
     * @param status   状态
     * @return 处方
     */
    @RpcService(mvcDisabled = true)
    RecipeResultBean cancelOrderByRecipeId(Integer recipeId, Integer status);

    /**
     * 根据处方信息获取物流单号
     *
     * @param recipeBean
     * @return
     */
    @RpcService(mvcDisabled = true)
    RecipeOrderBean getTrackingNumber(RecipeBean recipeBean);

    /**
     * 查询报销清单列表
     * @param reimbursementListReq
     * @return
     */
    @RpcService
    List<ReimbursementListResVO> findReimbursementList(ReimbursementListReqVO reimbursementListReq);

    /**
     * 查询报销清单详情
     * @param recipeId
     * @return
     */
    @RpcService
    ReimbursementDetailResVO findReimbursementDetail(Integer recipeId);

    /**
     * 第三方多处方创建订单使用
     * @param thirdCreateOrderReqDTO
     * @return
     */
    @RpcService
    Integer thirdCreateOrder(ThirdCreateOrderReqDTO thirdCreateOrderReqDTO);
 /**
     * 判断处方是否有效(到院取药-存储药柜)
     * @param cabinetVO
     * @return
     */
    @RpcService(mvcDisabled = true)
    CabinetVO validateCabinetRecipeStatus(CabinetVO cabinetVO);

    /**
     * 存储药柜放入通知
     * @param cabinetVO
     * @return
     */
    @RpcService(mvcDisabled = true)
    void putInCabinetNotice(CabinetVO cabinetVO);


}
