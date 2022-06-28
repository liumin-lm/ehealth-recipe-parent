package recipe.core.api.patient;


import com.ngari.common.dto.CheckRequestCommonOrderPageDTO;
import com.ngari.common.dto.SyncOrderVO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.dto.RecipeFeeDTO;
import com.ngari.recipe.dto.RecipeOrderDto;
import com.ngari.recipe.dto.ReimbursementDTO;
import com.ngari.recipe.dto.SkipThirdDTO;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.MedicalSettleInfoVO;
import com.ngari.recipe.recipe.model.RecipeOrderWaybillDTO;
import com.ngari.recipe.recipe.model.ReimbursementListReqVO;
import com.ngari.recipe.recipe.model.SkipThirdReqVO;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.util.annotation.RpcService;
import recipe.vo.ResultBean;
import recipe.vo.second.enterpriseOrder.DownOrderRequestVO;
import recipe.vo.second.enterpriseOrder.EnterpriseDownDataVO;

import java.util.List;

public interface IRecipeOrderBusinessService {
    /**
     * 更新核发药师信息
     *
     * @param recipeId
     * @param giveUser
     * @return
     */
    ResultBean updateRecipeGiveUser(Integer recipeId, Integer giveUser);

    /**
     * 订单状态更新
     *
     * @param updateOrderStatusVO 状态对象
     * @return
     */
    ResultBean updateRecipeOrderStatus(UpdateOrderStatusVO updateOrderStatusVO);

    SkipThirdDTO uploadRecipeInfoToThird(SkipThirdReqVO skipThirdReqVO);

    /**
     * 从微信模板消息跳转时 先获取一下是否需要跳转第三方地址
     * 或者处方审核成功后推送处方卡片消息时点击跳转(互联网)
     *
     * @return
     */
    SkipThirdDTO getSkipUrl(SkipThirdReqVO skipThirdReqVO);

    /**
     * 获取订单费用详情(邵逸夫模式专用)
     * @param orderCode
     * @return
     */
    List<RecipeFeeDTO> findRecipeOrderDetailFee(String orderCode);

    /**
     * 获取订单详情 (端用)
     * @param orderId
     * @return
     */
    RecipeOrderDto getRecipeOrderByBusId(Integer orderId);

    /**
     * 端同步历史数据使用
     * @param request
     * @return
     */
    CheckRequestCommonOrderPageDTO getRecipePageForCommonOrder(SyncOrderVO request);

    /**
     * 患者提交订单时更新pdf
     *
     * @param recipeId
     */
    void updatePdfForSubmitOrderAfter(Integer recipeId);

    /**
     *  根据订单号更新物流单号
     * @param orderCode 订单号
     * @param trackingNumber 物流单号
     * @return 是否成功
     */
    Boolean updateTrackingNumberByOrderCode(String orderCode, String trackingNumber);

    /**
     * 第三方查询平台处方订单信息
     * @param downOrderRequestVO 请求入参
     * @return 处方订单列表
     */
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
     * 根据处方号获取订单信息
     *
     * @param recipeCode 处方号
     * @param organId    机构id
     * @return 订单信息
     */
    RecipeOrder getTrackingNumber(String recipeCode, Integer organId);

    /**
     * 查询报销清单列表
     * @param reimbursementListReq
     * @return
     */
    List<ReimbursementDTO> findReimbursementList(ReimbursementListReqVO reimbursementListReq);

    /**
     * 查询报销清单详情
     * @param recipeId
     * @return
     */
    ReimbursementDTO findReimbursementDetail(Integer recipeId);

    MedicalSettleInfoVO getMedicalSettleInfo(Integer recipeId);

    /**
     * 获取当前订单用户下历史订单的运单信息
     * @param mpiId
     * @return
     */
    List<RecipeOrderWaybillDTO> findOrderByMpiId(String mpiId);
}
