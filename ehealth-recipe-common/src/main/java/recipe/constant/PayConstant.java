package recipe.constant;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 */
public interface PayConstant {
    /**
     * 微信支付返回code值类型
     * SUCCESS—支付成功
     * REFUND—转入退款
     * NOTPAY—未支付
     * CLOSED—已关闭
     * REVOKED—已撤销（刷卡支付）
     * USERPAYING--用户支付中
     * PAYERROR--支付失败(其他原因，如银行返回失败)
     */
    String RESULT_FAIL = "FAIL";
    String RESULT_WAIT = "WAIT_BUYER_PAY";
    String RESULT_SUCCESS = "SUCCESS";

    /**
     * 微信跨平台支付，付款目标机构未开通在线支付时的提示语
     */
    String ORGAN_NOT_OPEN_ONLINE_PAY_MSG = "抱歉，该机构暂未开通在线支付";

    /**
     * 消费费别 1 挂号 2结算 3制卡、病历手册 4住院预交金
     */
    String CONSUMER_TYPE_REGISTER = "1";
    String CONSUMER_TYPE_SETTLEMENT = "2";
    String CONSUMER_TYPE_MEDICAL_RECORD_CARD = "3";
    String CONSUMER_TYPE_PREPAY = "4";

    /**
     * 支付类型：1自费，2医保  与patientType的定义不相同
     */
    int PAY_TYPE_SELF_FINANCED = 1;
    int PAY_TYPE_MEDICAL_INSURANCE = 2;

    /**
     * payWay 医院his定义的支付方式：0-无第三方支付（即个人支付金额为0），1-支付宝，2-微信支付，3-银联卡支付
     */
    String HIS_PAY_WAY_NONE = "0";
    String HIS_PAY_WAY_ALIPAY = "1";
    String HIS_PAY_WAY_WEIXIN = "2";
    String HIS_PAY_WAY_UNIONPAY = "3";

    /**
     * 支付标志，
     * <item key="0" text="未付费"/>
     * <item key="1" text="付费"/>
     * <item key="2" text="退款中"/>
     * <item key="3" text="退款成功"/>
     * <item key="4" text="退款失败"/>
     */
    int PAY_FLAG_NOT_PAY = 0;
    int PAY_FLAG_PAY_SUCCESS = 1;
    int PAY_FLAG_REFUNDING = 2;
    int PAY_FLAG_REFUND_SUCCESS = 3;
    int PAY_FLAG_REFUND_FAIL = 4;


    int OUTPATIENT_OPTYPE_OP = 0;
    int OUTPATIENT_OPTYPE_RECIPE = 1;

    /**
     * 是否是跨公众号支付
     */
    String KEY_ISSTEPOVERWECHAT = "isStepOverMerchant";

    /**
     * 跨平台支付时返回给前端的授权地址
     */
    String KEY_WECHAT_AUTHURL = "weChatAuthUrl";

    /**
     * 待支付订单超过24小时，自动取消文本
     */
    int ORDER_OVER_TIME_HOURS = 24;
    String OVER_TIME_AUTO_CANCEL_TEXT = "超过" + ORDER_OVER_TIME_HOURS + "小时未支付，订单取消";

    /**
     * needPay 用于使用优惠券情况下，优惠后最终价格为0的情况判定，0表示不需要支付 后台已将该业务处理为支付完成状态，1表示需要支付 正常进行后续流程
     */
    String ORDER_NEED_PAY = "needPay";

    /**
     * 窗口（线下）退款模式，1线下退款，更改数据库状态；2线上执行除结算外的所有退款操作
     */
    int OFFLINE_REFUND_MODE_ONLY_CHANGE_STATUS = 1;
    int OFFLINE_REFUND_MODE_EXECUTE_EXCEPT_SETTLE = 2;
}
