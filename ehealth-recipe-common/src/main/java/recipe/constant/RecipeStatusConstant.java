package recipe.constant;

/**
 * 处方状态常量
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2016/4/27.
 */
public class RecipeStatusConstant {

    /**
     * 未知
     */
    public static final int UNKNOW = -9;

    /**
     *审核未通过(HIS平台)
     */
    public static final int CHECK_NOT_PASS = -1;

    /**
     *未签名
     */
    public static final int UNSIGN = 0;

    /**
     *待审核
     */
    public static final int UNCHECK = 1;

    /**
     *审核通过(医院平台)
     */
    public static final int CHECK_PASS = 2;

    /**
     *已支付
     */
    public static final int HAVE_PAY = 3;

    /**
     *配送中
     */
    public static final int IN_SEND = 4;

    /**
     *等待配送
     */
    public static final int WAIT_SEND = 5;

    /**
     *已完成
     */
    public static final int FINISH = 6;

    /**
     *审核通过(药师平台)
     */
    public static final int CHECK_PASS_YS = 7;

    /**
     *待药师审核
     */
    public static final int READY_CHECK_YS = 8;

    /**
     *已撤销
     */
    public static final int REVOKE = 9;

    /**
     *已删除(医生端历史处方不可见)
     */
    public static final int DELETE = 10;

    /**
     *取消：HIS写入失败
     */
    public static final int HIS_FAIL = 11;

    /**
     *取消：患者未取药
     */
    public static final int NO_DRUG = 12;

    /**
     *取消：患者未支付
     */
    public static final int NO_PAY = 13;

    /**
     *取消：患者未操作
     */
    public static final int NO_OPERATOR = 14;

    /**
     *取消：审核未通过(药师平台人工审核)
     */
    public static final int CHECK_NOT_PASS_YS = 15;

    /**
     *医院审核确认中
     */
    public static final int CHECKING_HOS = 16;

    /**
     *不存在的状态，用于微信发送,患者-未操作情况, 用于前一天提醒患者购药
     */
    public static final int PATIENT_NO_OPERATOR = 101;

    /**
     *不存在的状态，用于微信发送,患者-未支付情况, 用于前一天提醒患者购药 （医院取药-到院支付）
     */
    public static final int PATIENT_NO_PAY = 102;

    /**
     *不存在的状态，用于微信发送,患者货到付款配送成功给患者发微信推送
     */
    public static final int PATIENT_REACHPAY_FINISH = 103;

    /**
     *不存在的状态，用于微信发送,患者线上支付成功，待医院取药，给患者发微信推送
     */
    public static final int PATIENT_REACHHOS_PAYONLINE = 104;

    /**
     *不存在的状态，用于微信发送,患者线上支付成功，已医院取药，或者到院支付成功，已医院取药，给患者发微信
     */
    public static final int PATIENT_GETGRUG_FINISH = 105;

    /**
     *不存在的状态，患者选择配送到家-线上支付，审核不通过，给医生，患者发推送
     */
    public static final int CHECK_NOT_PASSYS_PAYONLINE = 106;

    /**
     *不存在的状态，患者选择配送到家-货到付款，审核不通过，给医生，患者发推送
     */
    public static final int CHECK_NOT_PASSYS_REACHPAY = 107;

    /**
     *不存在的状态，用于微信发送,HIS写入失败
     */
    public static final int PATIENT_HIS_FAIL = 108;

    /**
     *不存在的状态，由于药品库存不足，给患者发消息并退款
     */
    public static final int RECIPE_LOW_STOCKS = 109;

    /**
     *不存在的状态，药店取药提醒,用于前一天提醒患者购药
     */
    public static final int PATIENT_NODRUG_REMIND = 110;

    /**
     *不存在的状态，收货提醒,超过3天没有确认收货，给患者发推送
     */
    public static final int RECIPR_NOT_CONFIRM_RECEIPT = 111;

    /**
     * 不存在的状态, fromflag=2的处方药师审核不通过
     */
    public static final int PATIENT_HIS_YS_CHECK_NOT_PASS = 112;

    /**
     * 不存在的状态, fromflag=2的处方药师审核通过-配送到家
     */
    public static final int PATIENT_HIS_YS_CHECK_PASS_SEND = 113;

    /**
     * 不存在的状态, fromflag=2的处方药师审核通过-药店取药
     */
    public static final int PATIENT_HIS_YS_CHECK_PASS_TFDS = 114;

    /**
     * 不存在的状态, fromflag=2的处方取药完成
     */
    public static final int PATIENT_HIS_GET_FINISH = 115;
}
