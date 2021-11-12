package recipe.service;

import com.google.common.collect.Maps;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import com.ngari.revisit.process.service.IRecipeOnLineRevisitService;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import eh.cdr.constant.OrderStatusConstant;
import eh.cdr.constant.RecipeStatusConstant;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import recipe.ApplicationUtils;
import recipe.bean.RecipeCheckPassResult;
import recipe.constant.RecipeBussConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.hisservice.syncdata.SyncExecutorService;
import recipe.purchase.CommonOrder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HIS系统业务回调方法
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date: 2016/5/31.
 */
@Component
public class HisCallBackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HisCallBackService.class);

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
        Map<String, Object> recipeExtUpdateDataMap = Maps.newHashMap();
        Recipe recipe = recipeDAO.get(result.getRecipeId());
        if (null == recipe) {
            LOGGER.error("checkPassSuccess 处方对象不存在");
            return;
        }
        if (null != recipe.getStatus() && com.ngari.recipe.recipe.constant.RecipeStatusConstant.CHECK_PASS == recipe.getStatus()) {
            LOGGER.error("当前处方{}状态{}不能进行平台[checkPassSuccess]操作", result.getRecipeId(), recipe.getStatus());
            return;
        }
        // 更新处方拓展信息：his处方付费序号合集
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        Map<String, Object> extendMap = new HashedMap();
        extendMap.put("recipeCostNumber", result.getRecipeCostNumber());
        // 将取药窗口更新到ext表
        extendMap.put("pharmNo", result.getPharmNo());
        recipeExtendDAO.updateRecipeExInfoByRecipeId(Integer.valueOf(result.getRecipeId()), extendMap);
        LOGGER.info("checkPassSuccess.updateRecipeCostNumber,recipeId={},recipeCostNumber={}", result.getRecipeId(), result.getRecipeCostNumber());
        //todo---写死上海六院---在患者选完取药方式之后推送处方 第二次调用无需任何处理
        if (recipe.getClinicOrgan() == 1000899 && new Integer(1).equals(recipe.getChooseFlag())) {
            //日志记录
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "患者选择完购药方式之后推送处方成功");
            return;
        }
        if (StringUtils.isNotEmpty(result.getRecipeCode())) {
            attrMap.put("recipeCode", result.getRecipeCode());
        }
        if (StringUtils.isNotEmpty(result.getPatientID())) {
            //病人医院病历号
            attrMap.put("patientID", result.getPatientID());
        }
        if (StringUtils.isNotEmpty(result.getHisDiseaseSerial())) {
            recipeExtUpdateDataMap.put("hisDiseaseSerial", result.getHisDiseaseSerial());
        }
        //处方总金额， 外带药处方不做处理
        if (!Integer.valueOf(1).equals(recipe.getTakeMedicine()) && null != result.getTotalMoney()) {
            List<Recipedetail> recipedetailList = detailDAO.findByRecipeId(result.getRecipeId());
            if (CollectionUtils.isNotEmpty(recipedetailList) && CollectionUtils.isNotEmpty(result.getDetailList())) {
                if (recipedetailList.size() == result.getDetailList().size()) {
                    attrMap.put("totalMoney", result.getTotalMoney());
                    attrMap.put("actualPrice", result.getTotalMoney());
                }
            }
        }

        String memo = "HIS审核返回：写入his成功，审核通过";
        if (isCheckPass) {
            // 医保用户
            if (recipe.canMedicalPay()) {
                attrMap.put("giveMode", RecipeBussConstant.GIVEMODE_SEND_TO_HOME);
            }

            //其他平台处方状态不变
            if (0 == recipe.getFromflag()) {
                memo = "HIS审核返回：写入his成功(其他平台处方)";
            }
        } else {
            memo = "HIS审核返回：写入his成功，审核未通过";
        }
        //date 20200526
        //添加医院审方后保存审核日志
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), memo);

        recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), attrMap);
        if (!recipeExtUpdateDataMap.isEmpty()) {
            recipeExtendDAO.updateRecipeExInfoByRecipeId(recipe.getRecipeId(), recipeExtUpdateDataMap);
        }
        //更新复诊挂号序号、卡类型卡号等信息如果有
        updateRecipeRegisterID(recipe, result);
        //updateRecipepatientType(recipe);

        OrganDrugListService organDrugListService = ApplicationUtils.getRecipeService(OrganDrugListService.class);
        List<Recipedetail> recipedetails = result.getDetailList();
        if (CollectionUtils.isNotEmpty(recipedetails)) {
            Map<String, Object> detailAttrMap;
            for (Recipedetail detail : recipedetails) {
                if (null == detail.getRecipeDetailId()) {
                    continue;
                }
                detailAttrMap = Maps.newHashMap();
                detailAttrMap.put("drugGroup", detail.getDrugGroup());
                detailAttrMap.put("orderNo", detail.getOrderNo());
//                detailAttrMap.put("pharmNo", detail.getPharmNo());

                //因为从HIS返回回来的数据不是很全，所以要从DB获取一次
                Recipedetail recipedetail = detailDAO.getByRecipeDetailId(detail.getRecipeDetailId());
                //根据医院传入的价格更新药品总价
                if (null != recipedetail) {
                    detail.setDrugId(recipedetail.getDrugId());
                    BigDecimal drugCost = detail.getDrugCost();
                    //外带药处方不做处理
                    if (!Integer.valueOf(1).equals(recipe.getTakeMedicine()) && null != drugCost) {
                        detailAttrMap.put("drugCost", drugCost);
                        if (null != recipedetail.getUseTotalDose()) {
                            BigDecimal salePrice = drugCost.divide(BigDecimal.valueOf(recipedetail.getUseTotalDose()), 2, RoundingMode.UP);
                            detailAttrMap.put("salePrice", salePrice);
                            detail.setSalePrice(salePrice);
                        }
                    }
                }
                detailDAO.updateRecipeDetailByRecipeDetailId(detail.getRecipeDetailId(), detailAttrMap);
                /**更新药品最新的价格等*/
                organDrugListService.saveOrganDrug(recipe.getClinicOrgan(), detail);
            }
        }
        //date 20200507
        //调用医生重新签名的逻辑
        recipeService.retryDoctorSignCheck(result.getRecipeId());
    }


    private static void updateRecipeRegisterID(Recipe recipe, RecipeCheckPassResult result) {
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        Map<String, String> map = new HashMap<String, String>();

        //更新复诊挂号序号如果有
        if (null != recipe.getClinicId()) {

            //更新咨询扩展表recipeid字段
            if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource())) {
                IRevisitExService iRevisitExService = RevisitAPI.getService(IRevisitExService.class);
                RevisitExDTO revisitExDTO = iRevisitExService.getByConsultId(recipe.getClinicId());
                LOGGER.info("updateRecipeRegisterID revisitExDTO:{}", JSONUtils.toString(revisitExDTO));
                iRevisitExService.updateRecipeIdByConsultId(recipe.getClinicId(), recipe.getRecipeId());
                if (null != revisitExDTO) {
                    if (StringUtils.isNotEmpty(revisitExDTO.getRegisterNo())) {
                        result.setRegisterID(revisitExDTO.getRegisterNo());
                    }
                    if (StringUtils.isNotEmpty(revisitExDTO.getCardId()) && StringUtils.isNotEmpty(revisitExDTO.getCardType())) {
                        map.put("cardNo", revisitExDTO.getCardId());
                        map.put("cardType", revisitExDTO.getCardType());
                    }
                }
            } else if (RecipeBussConstant.BUSS_SOURCE_WZ.equals(recipe.getBussSource())) {
                IConsultExService exService = ConsultAPI.getService(IConsultExService.class);
                ConsultExDTO consultExDTO = exService.getByConsultId(recipe.getClinicId());
                LOGGER.info("updateRecipeRegisterID consultExDTO:{}", JSONUtils.toString(consultExDTO));
                exService.updateRecipeIdByConsultId(recipe.getClinicId(), recipe.getRecipeId());
                if (null != consultExDTO) {
                    if (StringUtils.isNotEmpty(consultExDTO.getRegisterNo())) {
                        result.setRegisterID(consultExDTO.getRegisterNo());
                    }
                    if (StringUtils.isNotEmpty(consultExDTO.getCardId()) && StringUtils.isNotEmpty(consultExDTO.getCardType())) {
                        map.put("cardNo", consultExDTO.getCardId());
                        map.put("cardType", consultExDTO.getCardType());
                    }
                }
            }

        }
        if (recipeExtend != null) {
            if (StringUtils.isNotEmpty(result.getRegisterID())) {
                map.put("registerID", result.getRegisterID());
                if (StringUtils.isNotEmpty(result.getMedicalType()) && StringUtils.isEmpty(recipeExtend.getMedicalType())) {
                    map.put("medicalType", result.getMedicalType());
                }
                if (StringUtils.isNotEmpty(result.getMedicalTypeText()) && StringUtils.isEmpty(recipeExtend.getMedicalTypeText())) {
                    map.put("medicalTypeText", result.getMedicalTypeText());
                }
                recipeExtendDAO.updateRecipeExInfoByRecipeId(recipe.getRecipeId(), map);
            }
        } else {
            recipeExtend = new RecipeExtend();
            recipeExtend.setRecipeId(recipe.getRecipeId());
            recipeExtend.setRegisterID(result.getRegisterID());
            recipeExtend.setMedicalType(result.getMedicalType());
            recipeExtend.setMedicalTypeText(result.getMedicalTypeText());
            recipeExtend.setCardNo(map.get("cardNo"));
            recipeExtend.setCardType(map.get("cardType"));
            if (StringUtils.isNotEmpty(recipeExtend.getRegisterID())) {
                recipeExtendDAO.saveRecipeExtend(recipeExtend);
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
        Map<String, Object> paramMap = Maps.newHashMap();
        //612: 表示无库存
        //614: 表示指定产地的没有库存
        boolean b = null != errCode && (612 == errCode || 614 == errCode);
        if (b) {
            paramMap.put("distributionFlag", 1);
        }
        recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.HIS_FAIL, paramMap);
        //日志记录
        RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECKING_HOS, RecipeStatusConstant.HIS_FAIL, "HIS审核返回：写入his失败[" + errCode + ":|" + errMsg + "]");
        //发送消息
        RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.HIS_FAIL);
        //复诊开方HIS确认失败 发送环信消息
        Recipe recipe = recipeDAO.get(recipeId);
        if (recipe == null) {
            return;
        }
        if (new Integer(2).equals(recipe.getBussSource())) {
            LOGGER.info("checkPassFail 复诊开方HIS确认失败 发送环信消息 recipeId:{}", recipeId);
            IRecipeOnLineRevisitService recipeOnLineRevisitService = RevisitAPI.getService(IRecipeOnLineRevisitService.class);
            recipeOnLineRevisitService.sendRecipeDefeat(recipe.getRecipeId(), recipe.getClinicId());
        }
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
                RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
                Map<String, Object> extendMap = new HashedMap();
                // 将取药窗口更新到ext表
                extendMap.put("pharmNo", detail.getPharmNo());
                recipeExtendDAO.updateRecipeExInfoByRecipeId(Integer.valueOf(detail.getRecipeId()), extendMap);
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
        LOGGER.error("havePayFail HIS线上支付返回：写入his失败*****recipe:" + JSONUtils.toString(recipe));

        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("chooseFlag", 0);
        //修改状态为 医院审核通过,使用户可以选择其他支付途径
        recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.CHECK_PASS, attrMap);

        //日志记录
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = orderDAO.getOrderByRecipeId(recipeId);
        RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.CHECK_PASS, "HIS线上支付返回：写入his失败，订单号:" + order.getOutTradeNo() + "，流水号:" + order.getTradeNo());

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
                    LOGGER.error("havePayRecipesFromHis HIS获取信息更新处方状态时存在相同处方数据,recipeCode:" + recipeCode + ",clinicOrgan:" + organId, e);
                }
                if (null != recipe) {
                    //对于已经在线上支付的处方不能直接取消
                    RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
                    if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                        if (new Integer(1).equals(recipeOrder.getPayFlag())) {
                            return;
                        }
                    }
                    Integer recipeId = recipe.getRecipeId();
                    Integer beforeStatus = recipe.getStatus();
                    if (null != recipeId) {
                        //先进行比较状态是否需要更新，可能HIS返回的仍是已支付的状态
                        if (beforeStatus == RecipeStatusConstant.HAVE_PAY) {
                            LOGGER.info("havePayRecipesFromHis recipeId=[{}], 已是已支付状态，无需更新", recipeId);
                            continue;
                        }
                        if (null == recipe.getPayDate()) {
                            attrMap.put("payDate", DateTime.now().toDate());
                        }
                        attrMap.put("giveMode", RecipeBussConstant.GIVEMODE_TO_HOS);
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
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);

        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("chooseFlag", 1);
        attrMap.put("payFlag", 1);
        attrMap.put("giveFlag", 1);
        attrMap.put("giveDate", DateTime.now().toDate());
        //以免进行处方失效前提醒
        attrMap.put("remindFlag", 1);


        for (String recipeCode : recipeCodes) {
            if (StringUtils.isNotEmpty(recipeCode)) {
                Recipe recipe = null;
                try {
                    recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, organId);
                } catch (Exception e) {
                    LOGGER.error("finishRecipesFromHis HIS获取信息更新处方状态时存在相同处方数据,recipeCode:" + recipeCode + ",clinicOrgan:" + organId, e);
                }
                if (null != recipe && !RecipeStatusEnum.RECIPE_STATUS_FINISH.getType().equals(recipe.getStatus())) {
                    // 已支付,只对到院取药的数据更新 未支付,全部都更新
                    String orderCode = recipe.getOrderCode();
                    if (Objects.isNull(orderCode)) {
                        finishForHis(recipe, attrMap, recipeDAO);
                        return;
                    }
                    RecipeOrder byOrderCode = recipeOrderDAO.getByOrderCode(orderCode);
                    if (Objects.isNull(byOrderCode)) {
                        finishForHis(recipe, attrMap, recipeDAO);
                        return;
                    }
                    if (Integer.valueOf(1).equals(byOrderCode.getPayFlag()) && RecipeBussConstant.GIVEMODE_TO_HOS.equals(recipe.getGiveMode())) {
                        finishForHis(recipe, attrMap, recipeDAO);
                        return;
                    }
                    if (Integer.valueOf(0).equals(byOrderCode.getPayFlag())) {
                        finishForHis(recipe, attrMap, recipeDAO);
                        return;
                    }

                }
            }
        }
    }

    private static void finishForHis(Recipe recipe, Map<String, Object> attrMap, RecipeDAO recipeDAO) {
        Integer recipeId = recipe.getRecipeId();
        Integer beforeStatus = recipe.getStatus();

        String logMemo = "HIS返回状态：医院取药已完成";
        Integer msgStatus = RecipeStatusConstant.PATIENT_GETGRUG_FINISH;
        if (null != recipeId) {
            if (null == recipe.getPayDate()) {
                attrMap.put("payDate", DateTime.now().toDate());
            }
            attrMap.put("giveMode", RecipeBussConstant.GIVEMODE_TO_HOS);
            attrMap.put("enterpriseId", null);

            Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.FINISH, attrMap);
            if (rs) {
                //线下支付完成后结束订单
                RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                orderService.finishOrder(recipe.getOrderCode(), null);
                //保存至电子病历
//                            RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
//                            recipeService.saveRecipeDocIndex(recipe);
                //日志记录
                RecipeLogService.saveRecipeLog(recipeId, beforeStatus, RecipeStatusConstant.FINISH, logMemo);
                //消息推送
                RecipeMsgService.batchSendMsg(recipeId, msgStatus);
                //更新pdf
                CommonOrder.finishGetDrugUpdatePdf(recipeId);
                //监管平台核销上传
                SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
                syncExecutorService.uploadRecipeVerificationIndicators(recipe.getRecipeId());
            }
        }
    }


}
