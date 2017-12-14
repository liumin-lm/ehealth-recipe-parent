package recipe.constant;

/**
 * 短信，推送，系统消息集合
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2016/5/9.
 * <p>
 * PC_ : 指发给PC端消息
 * APP_ : 指发送APP的消息
 * SMS_ : 短信信息
 * WX_ : 微信提醒
 * <p>
 * _doctor_ : 发送给医生的消息
 * _patient_ : 发送给患者的消息
 */
@Deprecated
public class RecipeMsgInfo {

    public static String PC_doctor_noOperator = "您给{patient}患者开的处方单已取消，请及时查看";

    public static String PC_doctor_noPay = "您给{patient}患者开的处方单由于患者未及时缴费已自动取消，请及时查看";

    public static String PC_doctor_recipeNotPass = "您给{patient}患者开的处方单审核未通过，请及时处理";

    public static String PC_doctor_hisFail = "您给{patient}患者开的处方单由于医院系统故障已自动取消，请及时查看";

    public static String PC_doctor_cancelRecipe_title = "处方单取消提醒";

    public static String PC_doctor_recipeNotPass_title = "处方单审核未通过提醒";

    public static String APP_doctor_cancelRecipe = "您有一张处方单已取消，请及时查看~";

    public static String APP_doctor_recipeNotPass = "您有一张处方单审核未通过，请及时查看~";

    public static String APP_doctor_checkRecipe = "您有一条新的待审核处方，请及时进行查看~";

    public static String SMS_doctor_recipeNotPass = "【纳里健康】小纳提醒：您给{patient}患者开的处方单审核未通过，请及时查看~";

    public static String SMS_patient_checkPass = "【纳里健康】小纳提醒：您收到一张待处理处方单，为避免订单失效给您带来不便，请于3天内进入纳里健康公众号首页，点击购药进行处理。若未关注纳里健康公众号（加V），请先关注。";

    public static String SMS_patient_cancelRecipe = "【纳里健康】小纳提醒：您于[2月24日]收到的处方单由于超过3天未处理，处方单已自动取消。";

    /**
     * 配送到家-线上支付:【纳里健康】小纳提醒：您于[2月24日]收到的处方单已取消，且该笔款项已为您退回，请及时联系医生！
     * 配送到家-货到付款:【纳里健康】小纳提醒：您于[2月24日]收到的处方单已取消，请及时联系医生！
     */
    public static String SMS_patient_checkNotPassYs = "药师审核失败";

    public static String SMS_patient_noOperator = "【纳里健康】小纳提醒：您于[2月24日]收到的处方单即将失效，请立即进入纳里健康公众号首页，点击购药进行处理。若未关注纳里健康公众号（加V），请先关注。";

    public static String SMS_patient_noPay = "【纳里健康】小纳提醒：您于[2月24日]收到的处方单即将失效，请务必明天携带患者身份证到医院缴费并取药。";

    public static String WX_patient_remark = "";

    /**
     * 处方单审核通过时给患者发微信推送
     */
    public static String WX_patient_checkPass = "您有一张待处理处方单，请及时查看。";

    /**
     * 处方单失效前给患者发送微信推送,未操作
     */
    public static String WX_patient_noOperator = "您于{checkDate}收到的处方单即将失效，请立即进入纳里健康公众号首页，点击购药进行处理。";

    /**
     * 处方单失效前给患者发送微信推送,未及时到医院缴费
     */
    public static String WX_patient_noPay = "您于{checkDate}收到的处方单即将失效，请务必明天携带患者身份证到医院缴费并取药。";

    /**
     * 患者选择配送到家-线上支付，审核不通过，给患者发微信推送
     */
    public static String WX_patient_checkNotPassYs_payonline = "您于{checkDate}收到的处方单已取消，且该笔款项将为您退回，请及时联系医生！";

    /**
     * 患者选择配送到家-货到付款，审核不通过，给患者发微信推送
     */
    public static String WX_patient_checkNotPassYs_reachpay = "您于{checkDate}收到的处方单已取消，请及时联系医生！";

    /**
     * 患者3天未处理，给患者发微信推送
     */
    public static String WX_patient_cancelRecipe = "您于{checkDate}收到的处方单由于超过3天未处理，处方单已自动取消。";

    /**
     * 患者货到付款配送中给患者发微信推送
     */
    public static String WX_patient_sending = "您于{checkDate}购买的药品正在配送中，请保持手机畅通。";

    /**
     * 患者已支付，但写入his失败，给患者发微信推送
     */
    public static String WX_patient_hisFail = "非常抱歉，由于医院系统故障，您于{checkDate}支付的处方单未能成功支付到医院，该笔款项将为您退回，建议您稍后尝试重新支付，或选择其他支付方式。";

    /**
     * 患者货到付款配送成功给患者发微信推送
     */
//    public static String WX_patient_reachPayFinish = "您于{checkDate}购买的药品已被签收，如有疑问，请通过" + ParamUtils.getParam(ParameterConstant.KEY_CUSTOMER_TEL,SystemConstant.CUSTOMER_TEL) + "联系小纳。";

    /**
     * 患者线上支付成功，待医院取药，给患者发微信推送
     */
    public static String WX_patient_reachHos_payOnline = "您于{checkDate}购买的药品已支付成功，为避免订单失效给您带来不便，请尽快到医院取药。";

    //患者线上支付成功，已医院取药，或者到院支付成功，已医院取药，给患者发微信
//    public static String WX_patient_getDrugFinish = "您于{checkDate}购买的药品已取药成功，如有疑问，请通过" + ParamUtils.getParam(ParameterConstant.KEY_CUSTOMER_TEL,SystemConstant.CUSTOMER_TEL) + "联系小纳。";
}
