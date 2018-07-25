package recipe.service;

import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.base.push.service.ISmsPushService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.DAOFactory;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;

import java.util.Collections;
import java.util.List;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/5/27.
 */
public class RecipeMsgService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeMsgService.class);

    private static ISmsPushService iSmsPushService = ApplicationUtils.getBaseService(ISmsPushService.class);


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
        if (CollectionUtils.isEmpty(recipesList)){
            return;
        }

        for (Recipe recipe : recipesList) {
            if (null == recipe) {
                continue;
            }
            Integer recipeId = recipe.getRecipeId();
            if (null == recipeId) {
                continue;
            }
            Integer organId = recipe.getClinicOrgan();
            if (RecipeStatusConstant.NO_PAY == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_NO_PAY, organId);
            } else if (RecipeStatusConstant.NO_OPERATOR == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_NO_OPERATOR, organId);
            } else if (RecipeStatusConstant.CHECK_NOT_PASS == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_CHECK_NOT_PASS, organId);
            } else if (RecipeStatusConstant.CHECK_NOT_PASSYS_PAYONLINE == afterStatus) {
                sendMsgInfo(recipeId, CHECK_NOT_PASS_YS_PAYONLINE, organId);
            } else if (RecipeStatusConstant.CHECK_NOT_PASSYS_REACHPAY == afterStatus) {
                sendMsgInfo(recipeId, CHECK_NOT_PASS_YS_REACHPAY, organId);
            } else if (RecipeStatusConstant.HIS_FAIL == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_HIS_FAIL, organId);
            } else if (RecipeStatusConstant.READY_CHECK_YS == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_READY_CHECK_YS, organId);
            } else if (RecipeStatusConstant.CHECK_PASS == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_CHECK_PASS, organId);
            } else if (RecipeStatusConstant.CHECK_PASS_YS == afterStatus) {
                String drugStoreName = "";
                if (RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())) {
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
                if (RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())) {
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
                sendMsgInfo(recipeId, RECIPE_REVOKE, organId, Integer.toString(afterStatus));
            } else if (RecipeStatusConstant.RECIPE_LOW_STOCKS == afterStatus) {
                sendMsgInfo(recipeId, RECIPE_LOW_STOCKS, organId, Integer.toString(afterStatus));
            } else if (RecipeStatusConstant.RECIPR_NOT_CONFIRM_RECEIPT == afterStatus) {
                sendMsgInfo(recipeId, RECIPR_NOT_CONFIRM_RECEIPT, organId, Integer.toString(afterStatus));
            }

        }

    }

    private static void sendMsgInfo(Integer recipeId, String bussType, Integer organId) {
        sendMsgInfo(recipeId, bussType, organId, null);
    }

    private static void sendMsgInfo(Integer recipeId, String bussType, Integer organId, String extendValue) {
        SmsInfoBean info = new SmsInfoBean();
        // 业务表主键
        info.setBusId(recipeId);
        // 业务类型
        info.setBusType(bussType);
        info.setSmsType(bussType);
        info.setClientId(null);
        info.setStatus(0);
        //0代表通用机构
        info.setOrganId(organId);
        info.setExtendValue(extendValue);
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
}
