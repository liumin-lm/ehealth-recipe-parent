package recipe.core.api.patient;


import com.ngari.common.dto.CheckRequestCommonOrderPageDTO;
import com.ngari.common.dto.SyncOrderVO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.vo.PreOrderInfoReqVO;
import com.ngari.recipe.vo.ShoppingCartReqVO;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import easypay.entity.vo.param.bus.MedicalPreSettleQueryReq;
import easypay.entity.vo.param.bus.SelfPreSettleQueryReq;
import recipe.vo.ResultBean;
import recipe.vo.greenroom.ImperfectInfoVO;
import recipe.vo.greenroom.RecipeRefundInfoReqVO;
import recipe.vo.second.CabinetVO;
import recipe.vo.second.CheckOrderAddressVo;
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

    /**
     * 获取当前订单用户下历史订单的运单信息
     * @param mpiId
     * @return
     */
    List<RecipeOrderWaybillDTO> findOrderByMpiId(String mpiId);

    /**
     * 更改订单物流信息
     * @param updateOrderStatusVO
     */
    void updateTrackingNumberByOrderId(UpdateOrderStatusVO updateOrderStatusVO);

    /**
     * 第三方创建订单
     * @param thirdCreateOrderReqDTO
     * @return
     */
    Integer thirdCreateOrder(ThirdCreateOrderReqDTO thirdCreateOrderReqDTO);

    /**
     * 根据订单批量修改发药药师
     * @param orderId
     * @param giveUser
     * @return
     */
    ResultBean updateOrderGiveUser(Integer orderId, Integer giveUser);
    /**
     * 判断处方是否有效(到院取药-存储药柜)
     * @param cabinetVO
     * @return
     */
    CabinetVO validateCabinetRecipeStatus(CabinetVO cabinetVO);

    /**
     * 存储药柜放入通知
     * @param cabinetVO
     * @return
     */
    void putInCabinetNotice(CabinetVO cabinetVO);

    /**
     * 补打发票-运营平台药品订单
     */
    Boolean makeUpInvoice(String orderCode);

    /**
     * 提供给支付调用自费预结算接口
     * @param busId
     * @return
     */
    SelfPreSettleQueryReq selfPreSettleQueryInfo(Integer busId);

    /**
     * 提供给支付调用医保预结算接口
     * @param busId
     * @return
     */
    MedicalPreSettleQueryReq medicalPreSettleQueryInfo(Integer busId);


    /**
     * 第三方获取订单预算信息
     * @param thirdOrderPreSettleReq
     * @return
     */
    ThirdOrderPreSettleRes thirdOrderPreSettle(ThirdOrderPreSettleReq thirdOrderPreSettleReq);


    /**
     * 获取未完善或完善标识
     * @param recipeBean
     * @return
     */
    Integer getImperfectFlag(com.ngari.recipe.recipe.model.RecipeBean recipeBean);

    /**
     * 获取购物车信息
     * @param mpiId
     * @return
     */
    List<ShoppingCartDetailDTO> getShoppingCartDetail(String mpiId);

    /**
     * 保存购物车信息
     * @param shoppingCartReqVO
     * @return
     */
    void saveRecipeBeforeOrderInfo(ShoppingCartReqVO shoppingCartReqVO);

    /**
     * 完善购物车信息
     * @param preOrderInfoReqVO
     * @return
     */
    String improvePreOrderInfo(PreOrderInfoReqVO preOrderInfoReqVO);

    /**
     * 获取加入购物车标识
     * @param recipeId
     * @return
     */
    Boolean getPreOrderFlag(Integer recipeId,String mpiId);
    /**
     * 批量获取未完善或完善标识
     * @param recipeBeans
     * @return
     */
    List<ImperfectInfoVO> batchGetImperfectFlag(List<com.ngari.recipe.recipe.model.RecipeBean> recipeBeans);

    String batchCheckSendAddressForOrder(List<CheckOrderAddressVo> checkOrderAddressVoList);

    ImperfectInfoVO getImperfectInfo(RecipeBean recipeBean);

    Integer getRecipeRefundCount(RecipeRefundInfoReqVO recipeRefundCountVO);

    /**
     * 门诊缴费合并支付患者端处方推送his
     * @param recipeIds
     */
    void submitRecipeHisV1(List<Integer> recipeIds);

    /**
     * 根据 二方id 查询订单列表
     *
     * @param clinicId   二方业务id
     * @param bussSource 开处方来源 1问诊 2复诊(在线续方) 3网络门诊
     * @return
     */
    List<RecipeOrder> orderListByClinicId(Integer clinicId, Integer bussSource);

    /**
     * 获取物流编码文件流
     *
     * @param orderCode
     */
    String logisticsOrderNo(String orderCode);

    /**
     * 患者端完成订单
     * @param orderCode
     * @return
     */
    void patientFinishOrder(String orderCode);
}
