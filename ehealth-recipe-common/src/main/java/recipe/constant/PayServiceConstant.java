package recipe.constant;

/**
 * Created by IntelliJ IDEA.
 * Description:
 * User: xiangyf
 * Date: 2017-04-24 11:16.
 */
public class PayServiceConstant {
    public static final String ORDER_PAY = "order.pay";
    public static final String ORDER_QUERY = "order.query";
    public static final String ORDER_CLOSE = "order.close";
    public static final String ORDER_REFUND = "order.refund";
    public static final String ORDER_REFUND_QUERY = "order.refund.query";
    public static final String ORDER_CANCEL = "order.cancel";

    public static final String ACCOUNT_FILE_DOWNLOAD = "account.file.download";

    public static final Integer ALIPAY = 1;//支付宝
    public static final Integer WXPAY = 2;//微信
    public static final Integer CMBPAY = 3;//一网通
    public static final Integer UNIONPAY = 4;//银联
    public static final Integer CCBPAY = 5;//建行
    public static final Integer ICBCPAY = 6;//工行
    public static final Integer WNPAY = 7;//卫宁支付平台
    public static final Integer BOCPAY = 8;//中国银行
    public static final Integer LCPAY = 9;//联空网络支付平台
    public static final Integer JSPRPAY = 10;//江苏省区域支付平台
    public static final Integer ICBCPAYV2 = 11;//工行开放平台
    public static final Integer HOPPAY = 12;//易联众统一支付平台
    public static final Integer HZMIPPAY = 13;//杭州市市民卡医保支付平台
    public static final Integer RCBPAY = 14;//农商行支付平台
    public static final Integer GFYPAY = 15;//广附院支付平台
    public static final Integer PWCPAY = 16;//平安智慧城支付平台
    public static final Integer YBMINIPAY = 17;//医保小程序支付
    public static final Integer HZWMPAY = 18;//汇智为民支付
    public static final Integer ABCPAY = 19;//农业银行
    public static final Integer BQPAY = 21;//扁鹊支付平台
    public static final Integer YTPAY = 23;//远图支付平台
    public static final Integer UNIONPAYV2 = 24;//银联智慧医疗支付平台
    public static final Integer YOUYPAY = 25;//甩付
    public static final Integer NETPAY = 26;//全民付
    public static final Integer BSPAY = 27;//创业支付平台
    public static final Integer WNYZTPAY = 28;//卫宁一账通
    public static final Integer JKWYPAY = 29;//健康无忧支付平台
    public static final Integer LIANLIANPAY = 33;//连连支付平台
    public static final Integer XNUNIONPAY = 34;//西宁统一支付平台
    public static final Integer JYTPAY = 35;//近医通支付平台

    public static final String PAY_NOTIFY_COMMON_URL = "/synPayCallBack/syn_notify";  // base支付完成通用回调地址
    public static final String PAY_CLOSE_URL = "/pageClose.html";                           // 郑州人民医院APP同步回调处理地址

}
