package com.ngari.recipe.recipeorder.service;

import com.ngari.recipe.IBaseService;
import com.ngari.recipe.common.RecipeBussResTO;
import com.ngari.recipe.common.RecipeListReqTO;
import com.ngari.recipe.common.RecipeListResTO;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.util.annotation.RpcService;
import eh.billcheck.vo.BillRecipeDetailVo;
import eh.billcheck.vo.RecipeBillRequest;
import eh.billcheck.vo.RecipeBillResponse;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/5.
 */
public interface IRecipeOrderService extends IBaseService<RecipeOrderBean> {

    /**
     * 预创建订单
     *
     * @param recipeIds 处方ID集合
     * @param extInfo   扩展参数
     *                  {"operMpiId":"当前操作者编码","addressId":"当前选中地址","payway":"支付方式（payway）","payMode":"处方支付方式",
     *                  "decoctionFlag":"1(1：代煎，0：不代煎)", "gfFeeFlag":"1(1：表示需要制作费，0：不需要)", “depId”:"指定药企ID",
     *                  "expressFee":"快递费","gysCode":"药店编码","sendMethod":"送货方式","payMethod":"支付方式"}
     * @return RecipeBussResTO<RecipeOrderBean>
     */
    @RpcService
    RecipeBussResTO<RecipeOrderBean> createBlankOrder(List<Integer> recipeIds, Map<String, String> extInfo);

    /**
     * 完成订单支付后调用
     *
     * @param orderCode 订单编号（非自增ID）
     * @param payFlag   支付结果 (PayConstant)
     * @param payMode   支付方式 (PayMode.dic)
     */
    @RpcService
    void finishOrderPay(String orderCode, int payFlag, Integer payMode);

    /**
     * 获取处方订单实际支付总金额
     *
     * @param actualPrice 处方实际支付金额， 没有优惠券则传NULL， 存在优惠券则传优惠之后的实际支付金额
     * @param order       订单对象
     * @return 处方订单实际支付金额
     */
    @RpcService
    BigDecimal countOrderTotalFeeWithCoupon(BigDecimal actualPrice, RecipeOrderBean order);

    /**
     * 更新订单信息
     *
     * @param orderCode 订单编号（非自增ID）
     * @param attrMap   修改属性集合（必须是RecipeOrder属性）
     */
    @RpcService
    void updateOrderInfo(String orderCode, Map<String, Object> attrMap);

    /**
     * 根据处方ID获取订单信息
     *
     * @param recipeId 处方ID
     * @return RecipeOrderBean
     */
    @RpcService
    RecipeOrderBean getOrderByRecipeId(int recipeId);

    /**
     * 根据商户订单号获取订单
     *
     * @param outTradeNo 订单号
     * @return RecipeOrderBean
     */
    @RpcService
    RecipeOrderBean getByOutTradeNo(String outTradeNo);

    /**
     * 查询某一支付状态下的订单
     *
     * @param request 查询条件
     *                payFlag: 参考PayConstant的支付值
     * @return RecipeListResTO<List<RecipeOrderBean>>
     */
    @RpcService
    RecipeListResTO<RecipeOrderBean> findByPayFlag(RecipeListReqTO request);

    /**
     * 药企是需要自己结算费用的，需要重新设置
     * @param enterpriseId
     * @param recipeIds
     * @return
     */
    @RpcService
    BigDecimal reCalculateRecipeFee(Integer enterpriseId, List<Integer> recipeIds, Map<String, String> extInfo);


    /**
     * 根据处方ID获取对应的订单，且不区分是否失效
     *
     * @param recipeId 处方ID
     * @return RecipeOrderBean
     */
    @RpcService
    RecipeOrderBean getRelationOrderByRecipeId(int recipeId);

    /**
     * 根据日期获取一天的订单支付信息
     *
     * @param request 日期
     * @return RecipeOrderBean
     */
    @RpcService
    public RecipeBillResponse<BillRecipeDetailVo> getRecipePayInfoByDate(RecipeBillRequest request);

    /**
     * 根据日期获取电子处方药企配送订单明细
     *
     * @param startTime 开始时间
     * @param endTime 截止时间
     * @param organId 机构ID
     * @param depId 药企ID
     * @return RecipeOrderBean
     */
    @RpcService
    Map<String, Object> recipeOrderDetailedStatistics(Date startTime, Date endTime, Integer organId, Integer depId, Integer drugId, String orderColumn, String orderType, int start, int limit);

    /**
     * 电子处方药企配送药品统计
     *
     * @param startTime 开始时间
     * @param endTime 截止时间
     * @param organId 机构ID
     * @param depId 药企ID
     * @return RecipeOrderBean
     */
    @RpcService
    public Map<String, Object> recipeDrugStatistics(Date startTime, Date endTime, Integer organId, Integer depId, Integer recipeId, String orderColumn, String orderType, int start, int limit);

    }
