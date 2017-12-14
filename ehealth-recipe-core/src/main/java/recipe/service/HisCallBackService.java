package recipe.service;

import com.google.common.collect.Maps;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.RecipeCheckPassResult;
import recipe.bean.RecipeResultBean;
import recipe.constant.*;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.util.ApplicationUtils;
import recipe.util.RecipeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HIS系统业务回调方法
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date: 2016/5/31.
 */
public class HisCallBackService {

    private static final Logger logger = LoggerFactory.getLogger(HisCallBackService.class);

    /**
     * 处方HIS审核通过成功
     *
     * @param result
     * @param isCheckPass
     */
    public static void checkPassSuccess(RecipeCheckPassResult result, boolean isCheckPass) {
        if (null == result || null == result.getRecipeId()) {
            return;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

        Map<String, Object> attrMap = Maps.newHashMap();
        Recipe recipe = recipeDAO.get(result.getRecipeId());
        if (null == recipe) {
            logger.error("checkPassSuccess 处方对象不存在");
            return;
        }
        if (StringUtils.isNotEmpty(result.getRecipeCode())) {
            attrMap.put("recipeCode", result.getRecipeCode());
        }
        if (StringUtils.isNotEmpty(result.getPatientID())) {
            //病人医院病历号
            attrMap.put("patientID", result.getPatientID());
        }
        //处方总金额， 外带药处方不做处理
        if (!Integer.valueOf(1).equals(recipe.getTakeMedicine()) && null != result.getTotalMoney()) {
            attrMap.put("totalMoney", result.getTotalMoney());
            attrMap.put("actualPrice", result.getTotalMoney());
        }

        Integer status = RecipeStatusConstant.CHECK_PASS;
        String memo = "HIS审核返回：写入his成功，审核通过";
        if (isCheckPass) {
            // 医保用户
            if (recipe.canMedicalPay()) {
                // 如果是中药或膏方处方不需要药师审核
                if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
                    status = RecipeStatusConstant.CHECK_PASS_YS;
                    memo = "HIS审核返回：写入his成功，药师审核通过";
                } else {
                    //可以进行医保支付，先去药师进行审核
                    status = RecipeStatusConstant.READY_CHECK_YS;
                    memo = "HIS审核返回：写入his成功，待药师审核";
                }
                attrMap.put("giveMode", RecipeBussConstant.GIVEMODE_SEND_TO_HOME);
            }

            //其他平台处方状态不变
            if (0 == recipe.getFromflag()) {
                status = recipe.getStatus();
                memo = "HIS审核返回：写入his成功(其他平台处方)";
            }
        } else {
            status = RecipeStatusConstant.CHECK_NOT_PASS;
            memo = "HIS审核返回：写入his成功，审核未通过";
        }

        recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), attrMap);
        List<Recipedetail> recipedetails = result.getDetailList();
        if (CollectionUtils.isNotEmpty(recipedetails)) {
            Map<Integer, BigDecimal> priceMap = Maps.newHashMap();
            Map<String, Object> detailAttrMap;
            for (Recipedetail detail : recipedetails) {
                if (null != detail.getRecipeDetailId()) {
                    detailAttrMap = Maps.newHashMap();
                    detailAttrMap.put("drugGroup", detail.getDrugGroup());
                    detailAttrMap.put("orderNo", detail.getOrderNo());
                    //根据医院传入的价格更新药品总价
                    BigDecimal drugCost = detail.getDrugCost();
                    //外带药处方不做处理
                    if (!Integer.valueOf(1).equals(recipe.getTakeMedicine()) && null != drugCost) {
                        detailAttrMap.put("drugCost", drugCost);
                        //因为从HIS返回回来的数据不是很全，所以要从DB获取一次
                        Recipedetail recipedetail = detailDAO.getByRecipeDetailId(detail.getRecipeDetailId());
                        if (null != recipedetail.getUseTotalDose()) {
                            BigDecimal salePrice = drugCost.divide(new BigDecimal(recipedetail.getUseTotalDose()), 2, RoundingMode.UP);
                            detailAttrMap.put("salePrice", salePrice);
                            priceMap.put(recipedetail.getDrugId(), salePrice);
                        }
                    }
                    detailDAO.updateRecipeDetailByRecipeDetailId(detail.getRecipeDetailId(), detailAttrMap);
                }
            }
            //更新医院-药品对应表的价格
            recipeService.updateDrugPrice(recipe.getClinicOrgan(), priceMap);
        }
        //写入his成功后，生成pdf并签名
        try {
            recipeService.generateRecipePdfAndSign(recipe.getRecipeId());
        } catch (Exception e) {
            logger.error("checkPassSuccess 签名服务异常. error={}", JSONUtils.toString(e.getStackTrace()));
        }
        //生成文件成功后再去更新处方状态
        recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), status, null);
        //日志记录
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), status, memo);

        //平台处方进行消息发送等操作
        if (1 == recipe.getFromflag()) {
            //发送消息
            RecipeMsgService.batchSendMsg(recipe.getRecipeId(), status);
        }

        //配送处方标记 1:只能配送 更改处方取药方式
        if (Integer.valueOf(1).equals(recipe.getDistributionFlag())) {
            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
            RecipeResultBean _result = hisService.recipeDrugTake(recipe.getRecipeId(), PayConstant.PAY_FLAG_NOT_PAY, null);
            if (RecipeResultBean.FAIL.equals(_result.getCode())) {
                logger.error("checkPassSuccess recipeId=[{}]更改取药方式失败，error=[{}]", recipe.getRecipeId(), _result.getError());
                throw new DAOException(ErrorCode.SERVICE_ERROR, "更改取药方式失败，错误:" + _result.getError());
            }
        }
    }

    /**
     * 处方HIS审核通过失败
     *
     * @param recipeId
     */
    public static void checkPassFail(Integer recipeId, Integer errCode, String errMsg) {
        if (null == recipeId) {
            return;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Map<String, Object> paramMap = new HashMap<>();
        //612: 表示无库存
        //614: 表示指定产地的没有库存
        if (null != errCode && (612 == errCode || 614 == errCode)) {
            paramMap.put("distributionFlag", 1);
        }
        recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.HIS_FAIL, paramMap);
        //日志记录
        RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECKING_HOS, RecipeStatusConstant.HIS_FAIL, "HIS审核返回：写入his失败[" + errCode + ":" + errMsg + "]");
        //发送消息
        RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.HIS_FAIL);
    }

    /**
     * 医院取药-线上支付-处方状态HIS修改为已支付 成功
     *
     * @param recipeId
     * @param detail
     */
    public static void havePaySuccess(Integer recipeId, Recipedetail detail) {
        if (null == recipeId) {
            return;
        }

        if (null != detail) {
            Map<String, Object> attrMap = Maps.newHashMap();
            attrMap.put("patientInvoiceNo", detail.getPatientInvoiceNo());
            attrMap.put("patientInvoiceDate", new DateTime().toDate());
            if (StringUtils.isNotEmpty(detail.getPharmNo())) {
                attrMap.put("pharmNo", detail.getPharmNo());
            }

            RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            recipeDetailDAO.updateRecipeDetailByRecipeId(recipeId, attrMap);
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null != recipe) {
            //日志记录
            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "HIS线上支付返回：写入his成功");
            //发送消息
            //到院取药方式才需要发送消息
            if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(recipe.getGiveMode())) {
                RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.PATIENT_REACHHOS_PAYONLINE);
            }
        }
    }

    /**
     * 医院取药-线上支付-处方状态HIS修改为已支付 失败
     *
     * @param recipeId
     */
    public static void havePayFail(Integer recipeId) {
        if (null == recipeId) {
            return;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        logger.error("havePayFail HIS线上支付返回：写入his失败*****recipe:" + JSONUtils.toString(recipe));

        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("chooseFlag", 0);
        //修改状态为 医院审核通过,使用户可以选择其他支付途径
        recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.CHECK_PASS, attrMap);

        //日志记录
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = orderDAO.getOrderByRecipeId(recipeId);
        RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(),
                RecipeStatusConstant.CHECK_PASS, "HIS线上支付返回：写入his失败，订单号:" + order.getOutTradeNo() + "，流水号:" + order.getTradeNo());

        //微信退款
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        recipeService.wxPayRefundForRecipe(1, recipeId, null);
    }

    /**
     * 从HIS获取已支付的数据回调
     *
     * @param recipeCodes 医院处方CODE
     */
    public static void havePayRecipesFromHis(List<String> recipeCodes, Integer organId) {
        if (CollectionUtils.isEmpty(recipeCodes) || null == organId) {
            return;
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        //数据共用
        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("chooseFlag", 1);
        attrMap.put("payFlag", 1);
        //以免进行处方失效前提醒
        attrMap.put("remindFlag", 1);

        String logMemo = "HIS返回状态：医院取药已支付";
        Integer msgStatus = RecipeStatusConstant.PATIENT_REACHHOS_PAYONLINE;

        for (String recipeCode : recipeCodes) {
            if (StringUtils.isNotEmpty(recipeCode)) {
                Recipe recipe = null;
                try {
                    recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, organId);
                } catch (Exception e) {
                    logger.error("havePayRecipesFromHis HIS获取信息更新处方状态时存在相同处方数据,recipeCode:" + recipeCode + ",clinicOrgan:" + organId);
                }
                if (null != recipe) {
                    Integer recipeId = recipe.getRecipeId();
                    Integer beforeStatus = recipe.getStatus();
                    if (null != recipeId) {
                        //先进行比较状态是否需要更新，可能HIS返回的仍是已支付的状态
                        if (beforeStatus == RecipeStatusConstant.HAVE_PAY) {
                            logger.info("havePayRecipesFromHis recipeId=[{}], 已是已支付状态，无需更新", recipeId);
                            continue;
                        }
                        if (null == recipe.getPayDate()) {
                            attrMap.put("payDate", DateTime.now().toDate());
                        }
                        attrMap.put("giveMode", RecipeBussConstant.GIVEMODE_TO_HOS);
                        attrMap.put("payMode", RecipeBussConstant.PAYMODE_TO_HOS);
                        attrMap.put("enterpriseId", null);

                        Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.HAVE_PAY, attrMap);
                        if (rs) {
                            //线下支付完成后取消订单
                            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                            orderService.cancelOrderByRecipeId(recipeId, OrderStatusConstant.CANCEL_AUTO);

                            //日志记录
                            RecipeLogService.saveRecipeLog(recipeId, beforeStatus, RecipeStatusConstant.HAVE_PAY, logMemo);
                            //消息推送
                            RecipeMsgService.batchSendMsg(recipeId, msgStatus);
                        }
                    }
                }
            }
        }

    }

    /**
     * 医院取药，从HIS获取已完成的数据回调
     *
     * @param recipeCodes 医院处方CODE
     */
    public static void finishRecipesFromHis(List<String> recipeCodes, Integer organId) {
        if (CollectionUtils.isEmpty(recipeCodes) || null == organId) {
            return;
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("chooseFlag", 1);
        attrMap.put("payFlag", 1);
        attrMap.put("giveFlag", 1);
        attrMap.put("giveDate", DateTime.now().toDate());
        //以免进行处方失效前提醒
        attrMap.put("remindFlag", 1);

        String logMemo = "HIS返回状态：医院取药已完成";
        Integer msgStatus = RecipeStatusConstant.PATIENT_GETGRUG_FINISH;

        for (String recipeCode : recipeCodes) {
            if (StringUtils.isNotEmpty(recipeCode)) {
                Recipe recipe = null;
                try {
                    recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, organId);
                } catch (Exception e) {
                    logger.error("finishRecipesFromHis HIS获取信息更新处方状态时存在相同处方数据,recipeCode:" + recipeCode + ",clinicOrgan:" + organId);
                }
                if (null != recipe) {
                    Integer recipeId = recipe.getRecipeId();
                    Integer beforeStatus = recipe.getStatus();
                    if (null != recipeId) {
                        if (null == recipe.getPayDate()) {
                            attrMap.put("payDate", DateTime.now().toDate());
                        }
                        attrMap.put("giveMode", RecipeBussConstant.GIVEMODE_TO_HOS);
                        attrMap.put("payMode", RecipeBussConstant.PAYMODE_TO_HOS);
                        attrMap.put("enterpriseId", null);

                        Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.FINISH, attrMap);
                        if (rs) {
                            //线下支付完成后取消订单
                            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                            orderService.cancelOrderByRecipeId(recipeId, OrderStatusConstant.CANCEL_AUTO);
                            //保存至电子病历
                            RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
                            recipeService.saveRecipeDocIndex(recipe);
                            //日志记录
                            RecipeLogService.saveRecipeLog(recipeId, beforeStatus, RecipeStatusConstant.FINISH, logMemo);
                            //消息推送
                            RecipeMsgService.batchSendMsg(recipeId, msgStatus);
                        }
                    }
                }
            }
        }
    }


}
