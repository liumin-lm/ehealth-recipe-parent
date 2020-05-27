package recipe.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.consult.common.service.IConsultService;
import com.ngari.consult.process.service.IRecipeOnLineConsultService;
import com.ngari.his.patient.mode.PatientQueryRequestTO;
import com.ngari.his.patient.service.IPatientHisService;
import com.ngari.home.asyn.model.BussCreateEvent;
import com.ngari.home.asyn.service.IAsynDoBussService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.base.constant.BussTypeConstant;
import eh.base.constant.ErrorCode;
import eh.cdr.constant.OrderStatusConstant;
import eh.cdr.constant.RecipeStatusConstant;
import eh.wxpay.constant.PayConstant;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditMode;
import recipe.audit.auditmode.AuditModeContext;
import recipe.bean.RecipeCheckPassResult;
import recipe.bussutil.RecipeUtil;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.purchase.CommonOrder;
import recipe.thread.PushRecipeToRegulationCallable;
import recipe.thread.RecipeBusiThreadPool;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private AuditModeContext auditMode;
    private static AuditModeContext auditModeContext;

    @PostConstruct
    public void init() {
        auditModeContext = auditMode;
    }

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
            LOGGER.error("checkPassSuccess 处方对象不存在");
            return;
        }
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
        //处方总金额， 外带药处方不做处理
        if (!Integer.valueOf(1).equals(recipe.getTakeMedicine()) && null != result.getTotalMoney()) {
            attrMap.put("totalMoney", result.getTotalMoney());
            attrMap.put("actualPrice", result.getTotalMoney());
        }

        String recipeMode = recipe.getRecipeMode();
        Integer status = RecipeStatusConstant.CHECK_PASS;

        /*if(RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)){
            status = RecipeStatusConstant.READY_CHECK_YS;
        }*/

        String memo = "HIS审核返回：写入his成功，审核通过";
        if (isCheckPass) {
            // 医保用户
            if (recipe.canMedicalPay()) {
                /*// 如果是中药或膏方处方不需要药师审核
                if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
                    status = RecipeStatusConstant.CHECK_PASS_YS;
                    memo = "HIS审核返回：写入his成功，药师审核通过";
                }*/

               /* else {
                    //可以进行医保支付，先去药师进行审核
                    status = RecipeStatusConstant.READY_CHECK_YS;
                    memo = "HIS审核返回：写入his成功，待药师审核";
                }*/
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

        //更新复诊挂号序号、卡类型卡号等信息如果有
        updateRecipeRegisterID(recipe,result);
        updateRecipepatientType(recipe);

        List<Recipedetail> recipedetails = result.getDetailList();
        if (CollectionUtils.isNotEmpty(recipedetails)) {
            Map<Integer, BigDecimal> priceMap = Maps.newHashMap();
            Map<String, Object> detailAttrMap;
            for (Recipedetail detail : recipedetails) {
                if (null != detail.getRecipeDetailId()) {
                    detailAttrMap = Maps.newHashMap();
                    detailAttrMap.put("drugGroup", detail.getDrugGroup());
                    detailAttrMap.put("orderNo", detail.getOrderNo());
                    detailAttrMap.put("pharmNo", detail.getPharmNo());
                    //根据医院传入的价格更新药品总价
                    BigDecimal drugCost = detail.getDrugCost();
                    //外带药处方不做处理
                    if (!Integer.valueOf(1).equals(recipe.getTakeMedicine()) && null != drugCost) {
                        detailAttrMap.put("drugCost", drugCost);
                        //因为从HIS返回回来的数据不是很全，所以要从DB获取一次
                        Recipedetail recipedetail = detailDAO.getByRecipeDetailId(detail.getRecipeDetailId());
                        if (recipedetail != null && null != recipedetail.getUseTotalDose()) {
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
//        try {
//            //写入his成功后，生成pdf并签名
//            recipeService.generateRecipePdfAndSign(recipe.getRecipeId());
//            //date 20200424
//            //判断当前处方的状态为签名失败不走下面逻辑
//            if(new Integer(28).equals(recipeService.getByRecipeId(recipe.getRecipeId()).getStatus())){
//                return;
//            }
//
//            //TODO 根据审方模式改变状态
//            auditModeContext.getAuditModes(recipe.getReviewType()).afterHisCallBackChange(status, recipe, memo);
//
//        } catch (Exception e) {
//            LOGGER.error("checkPassSuccess 签名服务或者发送卡片异常. ", e);
//        }
//
//        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipeMode)) {
//            //配送处方标记 1:只能配送 更改处方取药方式
//            if (Integer.valueOf(1).equals(recipe.getDistributionFlag())) {
//                try {
//                    RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
//                    RecipeResultBean result1 = hisService.recipeDrugTake(recipe.getRecipeId(), PayConstant.PAY_FLAG_NOT_PAY, null);
//                    if (RecipeResultBean.FAIL.equals(result1.getCode())) {
//                        LOGGER.warn("checkPassSuccess recipeId=[{}]更改取药方式失败，error=[{}]", recipe.getRecipeId(), result1.getError());
//                        //不能影响流程去掉异常
//                        /*throw new DAOException(ErrorCode.SERVICE_ERROR, "更改取药方式失败，错误:" + result1.getError());*/
//                    }
//                } catch (Exception e) {
//                    LOGGER.warn("checkPassSuccess recipeId=[{}]更改取药方式异常", recipe.getRecipeId(), e);
//                }
//            }
//        }
//        //2019/5/16 互联网模式--- 医生开完处方之后聊天界面系统消息提示
//        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)) {
//            /*//根据申请人mpiid，requestMode 获取当前咨询单consultId
//            IConsultService iConsultService = ApplicationUtils.getConsultService(IConsultService.class);
//            List<Integer> consultIds = iConsultService.findApplyingConsultByRequestMpiAndDoctorId(recipe.getRequestMpiId(),
//                    recipe.getDoctor(), RecipeSystemConstant.CONSULT_TYPE_RECIPE);
//            Integer consultId = null;
//            if (CollectionUtils.isNotEmpty(consultIds)) {
//                consultId = consultIds.get(0);
//            }*/
//            Integer consultId = recipe.getClinicId();
//            if (null != consultId) {
//                try {
//                    IRecipeOnLineConsultService recipeOnLineConsultService = ConsultAPI.getService(IRecipeOnLineConsultService.class);
//                    recipeOnLineConsultService.sendRecipeMsg(consultId, 3);
//                } catch (Exception e) {
//                    LOGGER.error("checkPassSuccess sendRecipeMsg error, type:3, consultId:{}, error:{}", consultId, e);
//                }
//
//            }
//        }
//        //推送处方到监管平台
//        RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(recipe.getRecipeId(), 1));

        //date 20200507
        //调用医生重新签名的逻辑
        recipeService.retryDoctorSignCheck(result.getRecipeId());


    }

    private static void updateRecipepatientType(Recipe recipe) {
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (StringUtils.isEmpty(recipeExtend.getPatientType())){
            String patientType = "1";
            //获取患者类型-后面让前置机传
            if (isMedicarePatient(recipe.getClinicOrgan(),recipe.getMpiid())){
                patientType = "2";
            }
            recipeExtendDAO.updateRecipeExInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("patientType", patientType));
        }
    }

    private static void updateRecipeRegisterID(Recipe recipe, RecipeCheckPassResult result) {
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        Map<String, String> map = new HashMap<String, String>();

        //更新复诊挂号序号如果有
        if (null != recipe.getClinicId()) {
            IConsultExService exService = ConsultAPI.getService(IConsultExService.class);
            ConsultExDTO consultExDTO = exService.getByConsultId(recipe.getClinicId());
            //更新咨询扩展表recipeid字段
            if (!(new Integer(3).equals(recipe.getBussSource()))){
                exService.updateRecipeIdByConsultId(recipe.getClinicId(),recipe.getRecipeId());
            }
            if (null != consultExDTO) {
                if (StringUtils.isNotEmpty(consultExDTO.getRegisterNo())){
                    result.setRegisterID(consultExDTO.getRegisterNo());
                }
                if (StringUtils.isNotEmpty(consultExDTO.getCardId())&&StringUtils.isNotEmpty(consultExDTO.getCardType())){
                    map.put("cardNo", consultExDTO.getCardId());
                    map.put("cardType", consultExDTO.getCardType());
                }
            }
        }
        if (recipeExtend != null) {
            if (StringUtils.isNotEmpty(result.getRegisterID())) {
                map.put("registerID", result.getRegisterID());
                map.put("medicalType", result.getMedicalType());
                map.put("medicalTypeText", result.getMedicalTypeText());
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

    public static Boolean isMedicarePatient(Integer organId, String mpiId) {
        //获取his患者信息判断是否医保患者
        IPatientHisService iPatientHisService = AppContextHolder.getBean("his.iPatientHisService", IPatientHisService.class);
        PatientService patientService = BasicAPI.getService(PatientService.class);
        PatientDTO patient = patientService.get(mpiId);
        if (patient == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "平台查询不到患者信息");
        }
        PatientQueryRequestTO req = new PatientQueryRequestTO();
        req.setOrgan(organId);
        req.setPatientName(patient.getPatientName());
        req.setCertificateType(patient.getCertificateType());
        req.setCertificate(patient.getCertificate());
        try {
            HisResponseTO<PatientQueryRequestTO> response = iPatientHisService.queryPatient(req);
            LOGGER.info("isMedicarePatient response={}", JSONUtils.toString(response));
            if (response != null) {
                PatientQueryRequestTO data = response.getData();
                if (data != null && "2".equals(data.getPatientType())) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("isMedicarePatient error" + e);
        }
        return false;
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
                    LOGGER.error("havePayRecipesFromHis HIS获取信息更新处方状态时存在相同处方数据,recipeCode:" + recipeCode + ",clinicOrgan:" + organId);
                }
                if (null != recipe) {
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
                    LOGGER.error("finishRecipesFromHis HIS获取信息更新处方状态时存在相同处方数据,recipeCode:" + recipeCode + ",clinicOrgan:" + organId);
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
                            //线下支付完成后结束订单
                            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                            orderService.finishOrder(recipe.getOrderCode(), recipe.getPayMode(), null);
                            //保存至电子病历
//                            RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
//                            recipeService.saveRecipeDocIndex(recipe);
                            //日志记录
                            RecipeLogService.saveRecipeLog(recipeId, beforeStatus, RecipeStatusConstant.FINISH, logMemo);
                            //消息推送
                            RecipeMsgService.batchSendMsg(recipeId, msgStatus);
                            //更新pdf
                            CommonOrder.finishGetDrugUpdatePdf(recipeId);
                        }
                    }
                }
            }
        }
    }


}
