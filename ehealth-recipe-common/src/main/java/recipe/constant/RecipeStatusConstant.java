package recipe.constant;

/**
 * 处方状态常量
 * todo 废弃常量 改用枚举 RecipeStatusEnum
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2016/4/27.
 */
@Deprecated
public class RecipeStatusConstant {

    /**
     * 未知
     */
    public static final int UNKNOW = -9;

    /**
     * 审核未通过(HIS平台)
     */
    public static final int CHECK_NOT_PASS = -1;

    /**
     * 未签名
     */
    public static final int UNSIGN = 0;

    /**
     * 待审核
     */
    public static final int UNCHECK = 1;

    /**
     *
     * 审核通过(医院平台)
     */
    public static final int CHECK_PASS = 2;

    /**
     * 已支付 (HIS回传状态)
     */
    public static final int HAVE_PAY = 3;

    /**
     * 配送中
     */
    public static final int IN_SEND = 4;

    /**
     * 等待配送
     */
    public static final int WAIT_SEND = 5;

    /**
     * 已完成
     */
    public static final int FINISH = 6;

    /**
     * 审核通过(药师平台)
     */
    public static final int CHECK_PASS_YS = 7;

    /**
     * 待药师审核
     */
    public static final int READY_CHECK_YS = 8;

    /**
     * 已撤销
     */
    public static final int REVOKE = 9;

    /**
     * 已删除(医生端历史处方不可见)
     */
    public static final int DELETE = 10;

    /**
     * 取消：HIS写入失败
     */
    public static final int HIS_FAIL = 11;

    /**
     * 取消：患者未取药
     */
    public static final int NO_DRUG = 12;

    /**
     * 取消：患者未支付
     */
    public static final int NO_PAY = 13;

    /**
     * 取消：患者未操作
     */
    public static final int NO_OPERATOR = 14;

    /**
     * 取消：审核未通过(药师平台人工审核)
     */
    public static final int CHECK_NOT_PASS_YS = 15;

    /**
     * 医院审核确认中
     */
    public static final int CHECKING_HOS = 16;

    /**
     * 取消：药店取药失败
     */
    public static final int RECIPE_FAIL = 17;

    /**
     * 处方已下载
     */
    public static final int RECIPE_DOWNLOADED = 18;

    /**
     * 取消：医保上传失败
     */
    public static final int RECIPE_MEDICAL_FAIL = 19;

    /**
     * 天猫可使用
     */
    public static final int EFFECTIVE = 2;

    /**
     * 天猫使用中
     */
    public static final int USING = 22;

    /**
     * 天猫已过期
     */
    public static final int EXPIRED = 20;

    /**
     * 天猫已退回
     */
    public static final int RETURNED = 23;

    /**
     * 医保上传确认中
     */
    public static final int CHECKING_MEDICAL_INSURANCE = 24;
    /**
     * 取消:医保上传确认中--三天未回传-已取消
     */
    public static final int NO_MEDICAL_INSURANCE_RETURN = 25;

    /**
     * 签名失败-医生
     */
    public static final int SIGN_ERROR_CODE_DOC = 26;

    /**
     * 签名失败-药师
     */
    public static final int SIGN_ERROR_CODE_PHA = 27;

    /**
     * 签名成功-医生
     */
    public static final int SIGN_SUCCESS_CODE_DOC = 28;

    /**
     * 签名成功-药师
     */
    public static final int SIGN_SUCCESS_CODE_PHA = 29;

    /**
     * 签名中-医生
     */
    public static final int SIGN_ING_CODE_DOC = 30;

    /**
     * 由于审方平台接口异常，处方单已取消
     */
    public static final int REVIEW_DRUG_FAIL = 43;
    /**
     * 签名中-药师
     */
    public static final int SIGN_ING_CODE_PHA = 31;

    /**
     * 未签名-药师
     */
    public static final int SIGN_NO_CODE_PHA = 32;
    /**
     * 不存在的状态，用于微信发送,患者-未操作情况, 用于前一天提醒患者购药
     */
    public static final int PATIENT_NO_OPERATOR = 101;

    /**
     * 不存在的状态，用于微信发送,患者-未支付情况, 用于前一天提醒患者购药 （医院取药-到院支付）
     */
    public static final int PATIENT_NO_PAY = 102;

    /**
     * 不存在的状态，用于微信发送,患者货到付款配送成功给患者发微信推送
     */
    public static final int PATIENT_REACHPAY_FINISH = 103;

    /**
     * 不存在的状态，用于微信发送,患者线上支付成功，待医院取药，给患者发微信推送
     */
    public static final int PATIENT_REACHHOS_PAYONLINE = 104;

    /**
     * 不存在的状态，用于微信发送,患者线上支付成功，已医院取药，或者到院支付成功，已医院取药，给患者发微信
     */
    public static final int PATIENT_GETGRUG_FINISH = 105;

    /**
     * 不存在的状态，患者选择配送到家-线上支付，审核不通过，给医生，患者发推送
     */
    public static final int CHECK_NOT_PASSYS_PAYONLINE = 106;

    /**
     * 不存在的状态，患者选择配送到家-货到付款，审核不通过，给医生，患者发推送
     */
    public static final int CHECK_NOT_PASSYS_REACHPAY = 107;

    /**
     * 不存在的状态，用于微信发送,HIS写入失败
     */
    public static final int PATIENT_HIS_FAIL = 108;

    /**
     * 不存在的状态，由于药品库存不足，给患者发消息并退款
     */
    public static final int RECIPE_LOW_STOCKS = 109;

    /**
     * 不存在的状态，药店取药提醒,用于前一天提醒患者购药
     */
    public static final int PATIENT_NODRUG_REMIND = 110;

    /**
     * 不存在的状态，收货提醒,超过3天没有确认收货，给患者发推送
     */
    public static final int RECIPR_NOT_CONFIRM_RECEIPT = 111;

    /**
     * 不存在的状态，用于处方订单取消
     */
    public static final int RECIPE_ORDER_CACEL = 112;

    /**
     * 不存在的状态，药店取药-无库存-准备药品
     */
    public static final int RECIPE_DRUG_NO_STOCK_READY = 121;

    /**
     * 不存在的状态，药店取药-无库存-到货
     */
    public static final int RECIPE_DRUG_NO_STOCK_ARRIVAL = 122;

    /**
     * 不存在的状态，药店取药-有库存-可取药
     */
    public static final int RECIPE_DRUG_HAVE_STOCK = 123;

    /**
     * 不存在的状态，药店取药-完成
     */
    public static final int RECIPE_TAKE_MEDICINE_FINISH = 124;

    /**
     * 不存在的状态，用于微信发送,患者提交审核推送,医生系统消息
     */
    public static final int RECIPE_REFUND_APPLY = 130;

    /**
     * 不存在的状态，用于微信发送,患者退费失败
     */
    public static final int RECIPE_REFUND_FAIL = 131;

    /**
     * 不存在的状态，用于微信发送,患者退费成功
     */
    public static final int RECIPE_REFUND_SUCC = 132;

    /**
     * 不存在的状态，用于微信发送,医生审核不通过
     */
    public static final int RECIPE_REFUND_AUDIT_FAIL = 133;


    /**
     * His或者药企审核不通过
     */
    public static final int RECIPE_REFUND_HIS_OR_PHARMACEUTICAL_AUDIT_FAIL = 135;
    /**
     * His或者药企审核通过
     */
    public static final int RECIPE_REFUND_HIS_OR_PHARMACEUTICAL_AUDIT_SUCCESS = 136;

    /**
     * 处方开方成功,提醒患者
     */
    public static final int PRESCRIBE_SUCCESS = 137;

    /**
     * 当药企/物流/运营平台配置返回了处方单的快递单号时，将给收货人的手机号码推送短信
     */
    public static final int EXPRESSINFO_REMIND = 138;


}
