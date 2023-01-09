package recipe.api.open;

import com.ngari.common.dto.CheckRequestCommonOrderPageDTO;
import com.ngari.common.dto.SyncOrderVO;
import com.ngari.platform.recipe.mode.RecipeBean;
import com.ngari.platform.recipe.mode.RecipeOrderBean;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.recipe.model.*;
import ctd.util.annotation.RpcService;
import recipe.vo.greenroom.ImperfectInfoVO;
import recipe.vo.greenroom.RecipeRefundInfoReqVO;
import recipe.vo.greenroom.RefundResultNotifyVO;
import recipe.vo.second.CabinetVO;
import recipe.vo.second.OrderPharmacyVO;
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
     * 第三方订单预算使用
     * @param thirdOrderPreSettleReq
     * @return
     */
    @RpcService
    ThirdOrderPreSettleRes thirdOrderPreSettle(ThirdOrderPreSettleReq thirdOrderPreSettleReq);
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


    /**
     * 获取未完善或完善标识
     * @param recipeBean
     * @return
     */
    @RpcService
    Integer getImperfectFlag(com.ngari.recipe.recipe.model.RecipeBean recipeBean);

    /**
     * 批量获取未完善或完善标识
     * @param recipeBeans
     * @return
     */
    @RpcService
    List<ImperfectInfoVO> batchGetImperfectFlag(List<com.ngari.recipe.recipe.model.RecipeBean> recipeBeans);

    /**
     * 获取未完善或完善标识、his处方付费序号合集
     * @param recipeBean
     * @return
     */
    @RpcService
    ImperfectInfoVO getImperfectInfo(com.ngari.recipe.recipe.model.RecipeBean recipeBean);

    /**
     * 医生端-我的数据查询退费量
     *
     * @param recipeRefundCountVO
     * @return
     */
    @RpcService
    Integer getRecipeRefundCount(RecipeRefundInfoReqVO recipeRefundCountVO);

    /**
     * 根据 二方id 查询订单列表
     *
     * @param clinicId   二方业务id
     * @param bussSource 开处方来源 1问诊 2复诊(在线续方) 3网络门诊
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<RecipeOrderVO> orderListByClinicId(Integer clinicId, Integer bussSource);

    /**
     * 运营平台-药品订单-更新是否打印发药清单和快递面单
     *
     * @param orderCode 订单编号
     * @param invoiceType 发票类型 1 打印发药清单 2 打印快递面单
     * @return
     */
    @RpcService
    Boolean updateInvoiceStatus(String orderCode, Integer invoiceType);


    /**
     * 提供给物流 ---- 根据订单编号查询处方药房信息
     * @param orderCode
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<OrderPharmacyVO> getPharmacyByOrderCode(String orderCode);

    /**
     * 校验订单是否在支付中
     * @param orderId
     * @return
     */
    @RpcService(mvcDisabled = true)
    Integer checkOrderPayState(Integer orderId);

    /**
     * 退费结果通知
     * @return
     */
    @RpcService(mvcDisabled = true)
    Integer refundResultNotify(RefundResultNotifyVO refundResultNotifyVO);
}
