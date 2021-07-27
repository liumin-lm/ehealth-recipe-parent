package recipe.service;

import com.google.common.collect.Maps;
import com.ngari.base.department.service.IDepartmentService;
import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.base.push.service.ISmsPushService;
import com.ngari.opbase.base.service.IDynamicLinkService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import recipe.ApplicationUtils;
import recipe.constant.ParameterConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeMsgEnum;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.service.common.RecipeCacheService;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;
import recipe.util.RecipeMsgUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2016/5/27.
 */
public class RecipeMsgService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeMsgService.class);

    private static ISmsPushService iSmsPushService = ApplicationUtils.getBaseService(ISmsPushService.class);

    private static RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);


    private static final int RECIPE_BUSSID = 10;

    /**
     * 消息业务类型
     */
    private static final String RECIPE_NO_PAY = "RecipeNoPay";

    private static final String RECIPE_NO_OPERATOR = "RecipeNoOperator";

    private static final String RECIPE_CHECK_NOT_PASS = "RecipeCheckNotPass";

    private static final String CHECK_NOT_PASS_YS_PAYONLINE = "NotPassYsPayOnline";

    private static final String CHECK_NOT_PASS_YS_REACHPAY = "NotPassYsReachPay";

    private static final String RECIPE_HIS_FAIL = "RecipeHisFail";

    private static final String RECIPE_READY_CHECK_YS = "RecipeReadyCheckYs";

    private static final String RECIPE_CHECK_PASS = "RecipeCheckPass";

    private static final String RECIPE_CHECK_PASS_YS = "RecipeCheckPassYs";

    private static final String RECIPE_NO_DRUG = "RecipeNoDrug";

    private static final String RECIPE_ORDER_CANCEL = "RecipeOrderCancel";

    private static final String RECIPE_REMIND_NO_OPERATOR = "RecipeRemindNoOper";

    private static final String RECIPE_REMIND_NO_PAY = "RecipeRemindNoPay";

    private static final String RECIPE_REMIND_NO_DRUG = "RecipeRemindNoDrug";

    private static final String RECIPE_REACHPAY_FINISH = "RecipeReachPayFinish";

    private static final String RECIPE_REACHHOS_PAYONLINE = "RecipeReachHosPayOnline";

    private static final String RECIPE_GETGRUG_FINISH = "RecipeGetDrugFinish";

    private static final String RECIPE_PATIENT_HIS_FAIL = "RecipePatientHisFail";

    private static final String RECIPE_IN_SEND = "RecipeInSend";

    private static final String RECIPE_REVOKE = "RecipeRevoke";

    private static final String RECIPE_LOW_STOCKS = "RecipeLowStocks";

    private static final String RECIPR_NOT_CONFIRM_RECEIPT = "RecipeNotConfirmReceipt";

    //武昌新增，无库存情况
    private static final String RECIPE_HOSSUPPORT_NOINVENTORY = "RecipeHosSupportNoInventory";

    //武昌新增，有库存情况
    private static final String RECIPE_HOSSUPPORT_INVENTORY = "RecipeHosSupportInventory";

    private static final String RECIPE_DRUG_NO_STOCK_READY = "RecipeDrugNoStockReady";

    private static final String RECIPE_DRUG_NO_STOCK_ARRIVAL = "RecipeDrugNoStockArrival";

    private static final String RECIPE_DRUG_HAVE_STOCK = "RecipeDrugHaveStock";

    private static final String RECIPE_TAKE_MEDICINE_FINISH = "RecipeTakeMedicineFinish";

    /**
     * 单个处方信息推送（根据处方ID）
     *
     * @param recipeId
     * @param afterStatus
     */
    public static void batchSendMsg(Integer recipeId, int afterStatus) {
        if (null != recipeId) {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            if (null != recipe) {
                batchSendMsg(recipe, afterStatus);
            }
        }
    }

    /**
     * 多个处方推送
     *
     * @param recipeIds
     * @param afterStatus
     */
    public static void batchSendMsg(List<Integer> recipeIds, int afterStatus) {
        if (CollectionUtils.isNotEmpty(recipeIds)) {
            List<Recipe> recipeList = DAOFactory.getDAO(RecipeDAO.class).findByRecipeIds(recipeIds);
            if (CollectionUtils.isNotEmpty(recipeList)) {
                batchSendMsgForNew(recipeList, afterStatus);
            }
        }
    }

    /**
     * 单个处方信息推送
     *
     * @param recipe
     * @param afterStatus
     */
    public static void batchSendMsg(Recipe recipe, int afterStatus) {
        batchSendMsgForNew(Collections.singletonList(recipe), afterStatus);
    }

    /**
     * 新款消息推送
     *
     * @param recipesList
     * @param afterStatus
     */
    public static void batchSendMsgForNew(List<Recipe> recipesList, int afterStatus) {
        if (CollectionUtils.isEmpty(recipesList)) {
            return;
        }

        Integer expiredDays = Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_VALIDDATE_DAYS, RecipeService.RECIPE_EXPIRED_DAYS.toString()));

        for (Recipe recipe : recipesList) {
            if (null == recipe) {
                continue;
            }
            Integer recipeId = recipe.getRecipeId();
            if (null == recipeId) {
                continue;
            }
            String recipeMode = recipe.getRecipeMode();
            Integer organId = recipe.getClinicOrgan();
            if (RecipeStatusConstant.NO_PAY == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_NO_PAY, organId);
            } else if (RecipeStatusConstant.NO_OPERATOR == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_NO_OPERATOR, organId);
            } else if (RecipeStatusConstant.CHECK_NOT_PASS == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_CHECK_NOT_PASS, organId);
            } else if (RecipeStatusConstant.CHECK_NOT_PASSYS_PAYONLINE == afterStatus) {
                sendMsgInfo(recipeId, CHECK_NOT_PASS_YS_PAYONLINE, organId);
            } else if (RecipeStatusConstant.RECIPE_ORDER_CACEL == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_ORDER_CANCEL, organId);
            } else if (RecipeStatusConstant.CHECK_NOT_PASSYS_REACHPAY == afterStatus) {
                sendMsgInfo(recipeId, CHECK_NOT_PASS_YS_REACHPAY, organId);
            } else if (RecipeStatusConstant.HIS_FAIL == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_HIS_FAIL, organId);
            } else if (RecipeStatusConstant.READY_CHECK_YS == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_READY_CHECK_YS, organId);
            } else if (RecipeStatusConstant.CHECK_PASS == afterStatus) {
                if (StringUtils.isEmpty(recipeMode) || RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipeMode)) {
                    sendMsgInfo(recipeId, RECIPE_CHECK_PASS, organId);
                } else if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)) {
                    Map<String, String> extendInfo = getRecipeCardInfo(recipe);
                    sendMsgInfo(recipeId, RECIPE_CHECK_PASS, organId, JSONUtils.toString(extendInfo));
                }
            } else if (RecipeStatusConstant.CHECK_PASS_YS == afterStatus) {
                String drugStoreName = "";
                if (RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getGiveMode())) {
                    drugStoreName = getDrugStoreName(recipeId);
                }
                sendMsgInfo(recipeId, RECIPE_CHECK_PASS_YS, organId, drugStoreName);
            } else if (RecipeStatusConstant.NO_DRUG == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_NO_DRUG, organId);
            } else if (RecipeStatusConstant.PATIENT_NO_OPERATOR == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_REMIND_NO_OPERATOR, organId);
            } else if (RecipeStatusConstant.PATIENT_NO_PAY == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_REMIND_NO_PAY, organId);
            } else if (RecipeStatusConstant.PATIENT_NODRUG_REMIND == afterStatus) {
                String drugStoreName = "";
                if (RecipeBussConstant.GIVEMODE_TFDS.equals(recipe.getGiveMode())) {
                    drugStoreName = getDrugStoreName(recipeId);
                }
                sendMsgInfo(recipeId, RECIPE_REMIND_NO_DRUG, organId, drugStoreName);
            } else if (RecipeStatusConstant.PATIENT_REACHPAY_FINISH == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_REACHPAY_FINISH, organId, Integer.toString(afterStatus));
            } else if (RecipeStatusConstant.PATIENT_REACHHOS_PAYONLINE == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_REACHHOS_PAYONLINE, organId, Integer.toString(afterStatus));
            } else if (RecipeStatusConstant.PATIENT_GETGRUG_FINISH == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_GETGRUG_FINISH, organId, Integer.toString(afterStatus));
            } else if (RecipeStatusConstant.PATIENT_HIS_FAIL == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_PATIENT_HIS_FAIL, organId, Integer.toString(afterStatus));
            } else if (RecipeStatusConstant.IN_SEND == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_IN_SEND, organId, Integer.toString(afterStatus));
            } else if (RecipeStatusConstant.REVOKE == afterStatus) {
                Map<String, String> extendValue = Maps.newHashMap();
                IDepartmentService iDepartmentService = ApplicationUtils.getBaseService(IDepartmentService.class);
                extendValue.put("departName", iDepartmentService.getNameById(recipe.getDepart()));
                sendMsgInfo(recipeId, RECIPE_REVOKE, organId, JSONUtils.toString(extendValue));
            } else if (RecipeStatusConstant.RECIPE_LOW_STOCKS == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_LOW_STOCKS, organId, Integer.toString(afterStatus));
            } else if (RecipeStatusConstant.RECIPR_NOT_CONFIRM_RECEIPT == afterStatus) {
                sendMsgInfo(recipeId, RECIPR_NOT_CONFIRM_RECEIPT, organId, Integer.toString(afterStatus));
            } else if (RecipeStatusConstant.RECIPE_DRUG_NO_STOCK_READY == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_DRUG_NO_STOCK_READY, organId, Integer.toString(afterStatus));
            } else if (RecipeStatusConstant.RECIPE_DRUG_NO_STOCK_ARRIVAL == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_DRUG_NO_STOCK_ARRIVAL, organId, Integer.toString(afterStatus));
            } else if (RecipeStatusConstant.RECIPE_DRUG_HAVE_STOCK == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_DRUG_HAVE_STOCK, organId, Integer.toString(afterStatus));
            } else if (RecipeStatusConstant.RECIPE_TAKE_MEDICINE_FINISH == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_TAKE_MEDICINE_FINISH, organId, Integer.toString(afterStatus));
            } else {
                //新处理方式
                Map<String, String> extendValue = Maps.newHashMap();
                RecipeMsgEnum msgEnum = RecipeMsgUtils.getEnumByStatus(afterStatus);
                switch (msgEnum) {
                    case RECIPE_YS_CHECKPASS_4STH:
                        getHosRecipeInfo(recipe, extendValue);
                        break;
                    case RECIPE_YS_CHECKPASS_4TFDS:
                        getHosRecipeInfo(recipe, extendValue);
                        //设置 expireDate 过期时间
                        extendValue.put("expireDate", DateConversion.formatDate(DateConversion.getDateAftXDays(recipe.getSignDate(), expiredDays)));
                        break;
                    case RECIPE_HAVE_PAY:
                        setRemarkFlag(recipe,extendValue);
                        break;
                    default:

                }

                sendMsgInfo(recipeId, msgEnum.getMsgType(), organId, JSONUtils.toString(extendValue));
            }

        }

    }

    /**
     * 通过枚举类型发送消息
     *
     * @param em
     * @param
     */
    public static void sendRecipeMsg(RecipeMsgEnum em, Integer recipeId) {
        RecipeDAO dao = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = dao.getByRecipeId(recipeId);
        Assert.notNull(recipe, "recipe must not be null");
        switch (em) {
            case RECIPE_EXPRESSFEE_REMIND_NOPAY:
                //获取配置动态链接
                String url = getRecipeExpressFeeRemindNoPayUrl(recipe);
                sendMsgInfo(recipeId, em.getMsgType(), recipe.getClinicOrgan(), url);
                return;
            default:
        }
    }

    /**
     * 通过枚举类型发送消息
     *
     * @param em
     * @param recipeList
     */
    public static void sendRecipeMsg(RecipeMsgEnum em, Recipe... recipeList) {
        Integer expiredDays = Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_VALIDDATE_DAYS, RecipeService.RECIPE_EXPIRED_DAYS.toString()));
        for (Recipe recipe : recipeList) {
            Integer recipeId = recipe.getRecipeId();
            Map<String, String> extendValue = Maps.newHashMap();
            switch (em) {
                case RECIPE_YS_CHECKNOTPASS_4HIS:
                    //需要获取手机号
                    getHosRecipeInfo(recipe, extendValue);
                    break;
                case RECIPE_FINISH_4HIS:
                    //需要获取手机号
                    getHosRecipeInfo(recipe, extendValue);
                    break;
                case RECIPE_YS_CHECKPASS_4STH:
                    getHosRecipeInfo(recipe, extendValue);
                    break;
                case RECIPE_YS_CHECKPASS_4FREEDOM:
                    getHosRecipeInfo(recipe, extendValue);
                    break;
                case RECIPE_YS_CHECKPASS_4TFDS:
                    getHosRecipeInfo(recipe, extendValue);
                    //设置 expireDate 过期时间
                    extendValue.put("expireDate", DateConversion.formatDate(DateConversion.getDateAftXDays(recipe.getSignDate(), expiredDays)));
                    break;
                case RECIPE_CANCEL_4HIS:
                    getHosRecipeInfo(recipe, extendValue);
                    //设置 overtime 超时时间
                    extendValue.put("overtime", "医院处方有效时间");
                    break;
                case RECIPE_HOSSUPPORT_NOINVENTORY:
                case RECIPE_HOSSUPPORT_INVENTORY:
                    IDepartmentService iDepartmentService = ApplicationUtils.getBaseService(IDepartmentService.class);
                    extendValue.put("departName", iDepartmentService.getNameById(recipe.getDepart()));
                    break;
                case RECIPE_EXPRESSFEE_REMIND_NOPAY:
                    //获取配置动态链接
                    String url = getRecipeExpressFeeRemindNoPayUrl(recipe);
                    sendMsgInfo(recipeId, em.getMsgType(), recipe.getClinicOrgan(), url);
                    return;
                case RECIPE_HOS_TAKE_MEDICINE:
                    RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
                    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
                    OrganService organService = BasicAPI.getService(OrganService.class);
                    OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());

                    if (!Objects.isNull(recipeExtend) && StringUtils.isNotEmpty(recipeExtend.getPharmNo())) {
                        extendValue.put("pharmNo", organDTO.getName() + recipeExtend.getPharmNo() + "取药窗口");
                    }
                    break;
                default:

            }


            sendMsgInfo(recipeId, em.getMsgType(), recipe.getClinicOrgan(), JSONUtils.toString(extendValue));
        }
    }

    private static void setRemarkFlag(Recipe recipe, Map<String, String> extendValue){
        String remarkFlag = "remarkflag=0";
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeOrder order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            // 医保支付的卫宁付情况下需要发送支付内容
            if (Objects.nonNull(order) && Objects.nonNull(order.getHealthInsurancePayContent()) &&
                    "111".equals(order.getWxPayWay()) && !new Integer(0).equals(order.getOrderType())) {

                String healthInsurancePayContent = order.getHealthInsurancePayContent();
                Map<String, String> healthInsurancePayContentMap = JSONUtils.parse(healthInsurancePayContent, Map.class);
                if (Objects.nonNull(healthInsurancePayContentMap)) {
                    remarkFlag = "remarkflag=1";
                    // 总金额
                    Double preSettletotalAmount = order.getPreSettletotalAmount();
                    // 现金支付
                    String xjzf = StringUtils.defaultString(healthInsurancePayContentMap.get("xjzf"), "0");
                    // 医保统筹支付
                    String ybtczf = StringUtils.defaultString(healthInsurancePayContentMap.get("ybtczf"), "0");
                    // 个人账户支付
                    String grzhzf = StringUtils.defaultString(healthInsurancePayContentMap.get("grzhzf"), "0");
                    // 附加支付
                    String fjzf = StringUtils.defaultString(healthInsurancePayContentMap.get("fjzf"), "0");
                    // 分类自负
                    String flzifu = StringUtils.defaultString(healthInsurancePayContentMap.get("flzifu"), "0");
                    // 现金自负
                    String xjzifu = StringUtils.defaultString(healthInsurancePayContentMap.get("xjzifu"), "0");
                    // 自费
                    String zifei = StringUtils.defaultString(healthInsurancePayContentMap.get("zifei"), "0");
                    // 当年账户余额
                    String dnzhye = StringUtils.defaultString(healthInsurancePayContentMap.get("dnzhye"), "0");
                    // 历年账户余额
                    String lnzhye = StringUtils.defaultString(healthInsurancePayContentMap.get("lnzhye"), "0");

                    extendValue.put("preSettletotalAmount", preSettletotalAmount.toString());
                    extendValue.put("xjzf", xjzf);
                    extendValue.put("ybtczf", ybtczf);
                    extendValue.put("grzhzf", grzhzf);
                    extendValue.put("fjzf", fjzf);
                    extendValue.put("flzifu", flzifu);
                    extendValue.put("xjzifu", xjzifu);
                    extendValue.put("zifei", zifei);
                    extendValue.put("dnzhye", dnzhye);
                    extendValue.put("lnzhye", lnzhye);
                }
            }
        }
        extendValue.put("remarkflag",remarkFlag);
    }
    private static String getRecipeExpressFeeRemindNoPayUrl(Recipe recipe) {
        //获取配置动态链接
        IDynamicLinkService dynamicLinkService = AppContextHolder.getBean("opbase.dynamicLinkService", IDynamicLinkService.class);
        return dynamicLinkService.getLinkUrlByLinkKey(recipe.getClinicOrgan() + "_RecipeEFRemindUrl");
    }

    private static void sendMsgInfo(Integer recipeId, String bussType, Integer organId) {
        sendMsgInfo(recipeId, bussType, organId, null);
    }

    private static void sendMsgInfo(Integer recipeId, String bussType, Integer organId, String extendValue) {
        if (StringUtils.isEmpty(bussType)) {
            return;
        }

        Integer clientId = null;
        Integer urt = null;
       /* //处方审核通过添加clientId
        if (bussType != null && bussType.equals(RECIPE_CHECK_PASS)) {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            clientId = recipe.getCurrentClient();
        }*/
        if (recipeId != null && recipeId != 0) {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            urt = recipe.getRequestUrt();
        }

        SmsInfoBean info = new SmsInfoBean();
        // 业务表主键
        info.setBusId(recipeId);
        // 业务类型
        info.setBusType(bussType);
        info.setSmsType(bussType);
        info.setClientId(clientId);
        info.setUrt(urt);
        info.setStatus(0);
        //0代表通用机构
        info.setOrganId(organId);
        info.setExtendValue(extendValue);
        LOGGER.info("send msg : {}", JSONUtils.toString(info));
        iSmsPushService.pushMsg(info);
    }

    /**
     * 到店取药时，获取药店名称
     *
     * @param recipeId
     * @return
     */
    private static String getDrugStoreName(Integer recipeId) {
        String drugStoreName = "";
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = recipeOrderDAO.getOrderByRecipeId(recipeId);
        if (null != order) {
            drugStoreName = order.getDrugStoreName();
        }

        return drugStoreName;
    }

    /**
     * 互联网医院获取就诊卡扩展信息
     *
     * @param recipe
     */
    private static Map<String, String> getRecipeCardInfo(Recipe recipe) {
        Map<String, String> extendValue = Maps.newHashMap();
        Integer recipeId = recipe.getRecipeId();
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (recipeExtend != null && StringUtils.isNotEmpty(recipeExtend.getCardNo()) && StringUtils.isNotEmpty(recipeExtend.getCardTypeName())) {

            String cardNo = recipeExtend.getCardNo();
            String cardTypeName = recipeExtend.getCardTypeName();
            extendValue.put("cardTypeName", cardTypeName);
            extendValue.put("cardNo", cardNo);
        }

        return extendValue;
    }

    /**
     * HOS处方消息扩展信息
     *
     * @param recipe
     * @param extendValue
     */
    private static void getHosRecipeInfo(Recipe recipe, Map<String, String> extendValue) {
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            if (null != order) {
                extendValue.put("patientAddress", order.getAddress4());
                extendValue.put("patientTel", order.getRecMobile());
                extendValue.put("pharmacyName", order.getDrugStoreName());
                extendValue.put("pharmacyAddress", order.getDrugStoreAddr());
            }
        }
    }

    /**
     * 医保支付完成后消息发送
     *
     * @param recipeId
     * @param success
     */
    public static void doAfterMedicalInsurancePaySuccess(int recipeId, boolean success) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null != recipe) {
            SmsInfoBean smsInfo = new SmsInfoBean();
            smsInfo.setBusId(recipeId);
            //MessagePushExecutorConstant.PAYRESULT_RECIPE
            smsInfo.setBusType("RecipePayResult");
            smsInfo.setSmsType("RecipePayResult");
            smsInfo.setOrganId(recipe.getClinicOrgan());
            smsInfo.setExtendValue(String.valueOf(success));
            iSmsPushService.pushMsg(smsInfo);
            LOGGER.info("doAfterMedicalInsurancePaySuccess success, recipeId[{}]", recipeId);
        } else {
            LOGGER.info("doAfterMedicalInsurancePaySuccess recipe is null, recipeId[{}]", recipeId);
        }
    }

    /**
     * 发送用药指导模板消息---
     * <p>
     * 场景一-扫码后触发-微信事件消息--WXCallbackListenerImpl》onEvent
     * wxservice(扫码) -> recipe(得到参数) -> 前置机(获取his药品相关信息) -> recipe(第三方获取跳转url) —> wxservice(推送微信模板事件消息)
     */
    @Deprecated
    public static void sendMedicationGuideMsg(String appId, String templateId, String openId, String url, Map<String, Object> data) {
        //已经移到sms里处理
    }

    /**
     * 发送用药指导模板消息--
     * 场景三-线下开处方线上推送消息--前提患者已在公众号注册过
     * 前置机(推送his处方药品等信息)->recipe(获取第三方url)->sms(发送微信模板消息)
     */
    public static void sendMedicationGuideMsg(Map<String, Object> param) {
        Integer organId = MapValueUtil.getInteger(param, "organId");
        sendMsgInfo(0, "medicationGuidePush", organId, JSONUtils.toString(param));
    }

    /**
     * 发送第三方链接的模板消息
     */
    public static void sendRecipeThirdMsg(Map<String, Object> param) {
        Integer organId = MapValueUtil.getInteger(param, "organId");
        sendMsgInfo(0, "recipeThirdPush", organId, JSONUtils.toString(param));
    }
}
