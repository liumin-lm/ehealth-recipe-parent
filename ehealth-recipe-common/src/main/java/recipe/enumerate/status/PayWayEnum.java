package recipe.enumerate.status;

import recipe.constant.PayServiceConstant;

/**
 * Created by IntelliJ IDEA.
 * Description: payWay in base ,payWay desc ,payWay in platform ,service ,payType ,payType desc
 * User: xiangyf
 * Date: 2017-04-24 11:18.
 */
public enum PayWayEnum {
    UNKNOW("-1", "未知", "", null, "未知"),

    //支付宝支付
    ALIPAY_WAP("07", "支付宝wap支付", "WAP", PayServiceConstant.ALIPAY, "支付宝"),
    ALIPAY_WEB("08", "支付宝web支付", "WEB", PayServiceConstant.ALIPAY, "支付宝"),
    ALIPAY_JSAPI("09", "支付宝JSAPI支付", "JSAPI", PayServiceConstant.ALIPAY, "支付宝"),
    ALIPAY_WITHHOLD("31", "支付宝代扣支付", "", PayServiceConstant.ALIPAY, "支付宝"),
    ALIPAY_QR_CODE("32", "支付宝扫码（二维码）支付", "QR", PayServiceConstant.ALIPAY, "支付宝"),
    ALIPAY_AUTH_CODE("33", "支付宝条码、声波支付", "AUTH", PayServiceConstant.ALIPAY, "支付宝"),
    ALIPAY_APP("34", "支付宝APP支付", "APP", PayServiceConstant.ALIPAY, "支付宝"),

    //微信支付
    WEIXIN_JSAPI("40", "微信wap支付", "JSAPI", PayServiceConstant.WXPAY, "微信"),
    WEIXIN_APP("41", "微信app支付", "APP", PayServiceConstant.WXPAY, "微信"),
    WEIXIN_WEB("42", "微信web支付", "WEB", PayServiceConstant.WXPAY, "微信"),
    WEIXIN_QR_CODE("43", "微信扫码支付", "QR", PayServiceConstant.WXPAY, "微信"),
    WEIXIN_AUTH_CODE("44", "微信条码支付", "AUTH", PayServiceConstant.WXPAY, "微信"),
    WEIXIN_WAP("45", "微信H5支付", "WAP", PayServiceConstant.WXPAY, "微信"),

    //招商银行一网通支付
    CMB_WAP("50", "一网通wap支付", "WAP", PayServiceConstant.CMBPAY, "一网通"),

    //建行网页支付
    CCB_WAP("60", "建行wap支付", "WAP", PayServiceConstant.CCBPAY, "建行"),
    CCB_JSAPI("61", "建行微信JSAPI支付", "JSAPI", PayServiceConstant.CCBPAY, "建行"),

    //工行支付
    ICBC_JSAPI("70", "工行jsapi支付", "JSAPI", PayServiceConstant.ICBCPAY, "工行"),

    ICBC_WAP("71", "工行WAP支付", "WAP", PayServiceConstant.ICBCPAYV2, "工行V2"),
    ICBC_SIGN("72", "工行钱包免密支付", "SIGN", PayServiceConstant.ICBCPAYV2, "工行V2"),

    //中国银行支付
    BOC_WAP("80", "中国银行WAP支付", "WAP", PayServiceConstant.BOCPAY, "中国银行"),

    //联空网络支付平台
    LC_WAP("100", "联空微信WAP支付", "WAP", PayServiceConstant.LCPAY, "联空微信"),

    //卫宁付
    WN_JSAPI("110", "卫宁付JSAPI支付", "JSAPI", PayServiceConstant.WNPAY, "卫宁付"),
    WN_WAP("111", "卫宁付一码付", "WAP", PayServiceConstant.WNPAY, "卫宁付"),
    WN_ABC_WAP("115", "卫宁农行微信支付", "WAP", PayServiceConstant.WNPAY, "卫宁付"),
    WN_APP("112", "卫宁付微信APP支付", "APP", PayServiceConstant.WNPAY, "卫宁付"),
    WN_QR("114", "卫宁付徽商银行", "QR", PayServiceConstant.WNPAY, "卫宁付"),
    WN_MINIAPP("116", "卫宁付小程序支付", "MINIAPP", PayServiceConstant.WNPAY, "卫宁付"),

    WNYZT_SIGN("117", "卫宁一账通签约支付", "SIGN", PayServiceConstant.WNYZTPAY, "卫宁一账通"),

    //汇智为民支付
    HZWM_JSAPI("113", "汇智为民微信JSAPI支付", "JSAPI", PayServiceConstant.HZWMPAY, "汇智为民支付"),


    //就诊卡支付
    VISIT_CARD_PAY("90", "就诊卡支付", "", null, "医院就诊卡支付"),

    ENPU_YB_PAY("91", "恩普医保支付", "APP", null, "恩普APP支付"),
    JSPR_PAY("92", "江苏省区域支付", "WAP", PayServiceConstant.JSPRPAY, "江苏省区域支付"),
    JSPR_PAY_APP("93", "江苏省区域支付", "WAP", PayServiceConstant.JSPRPAY, "江苏省区域支付"),
    HOP_PAY_JSAPI("94", "易联众微信JSAPI支付", "JSAPI", PayServiceConstant.HOPPAY, "易联众统一支付"),
    HOP_PAY_WAP("96", "易联众微信H5支付", "WAP", PayServiceConstant.HOPPAY, "易联众统一支付"),
    HOP_PAY_QR("101", "易联众微信二维码支付", "QR", PayServiceConstant.HOPPAY, "易联众统一支付"),

    //银联支付
    UNION_PAY_WAP("95", "银联WAP支付", "WAP", PayServiceConstant.UNIONPAY, "银联支付"),

    //杭州市市民卡支付
    HZMIP_PAY_WAP("120", "杭州市市民卡WAP支付", "WAP", PayServiceConstant.HZMIPPAY, "市民卡支付"),

    //农商行支付
    RCB_PAY_WAP("130", "农商行微信H5支付", "WAP", PayServiceConstant.RCBPAY, "农商行支付"),

    //农业银行
    ABC_PAY_WAP("135", "农业银行微信JSAPI支付", "JSAPI", PayServiceConstant.ABCPAY, "农业银行"),

    //广附院支付平台
    GFY_PAY_JSAPI("140", "广附院支付平台JSAPI支付", "JSAPI", PayServiceConstant.GFYPAY, "广附院支付"),

    //平安智慧城支付平台
    PWC_PAY_APP("150", "平安智慧城支付平台APP支付", "APP", PayServiceConstant.PWCPAY, "平安智慧城支付平台"),

    //医保小程序支付
    YBMINI_PAY_JSAPI("160", "医保小程序JSAPI支付", "JSAPI", PayServiceConstant.YBMINIPAY, "医保小程序支付"),

    BQ_PAY_JSAPI("161", "扁鹊支付平台JSAPI支付", "JSAPI", PayServiceConstant.BQPAY, "扁鹊支付平台"),
    BQ_PAY_WAP("162", "扁鹊支付平台WAP支付", "WAP", PayServiceConstant.BQPAY, "扁鹊支付平台"),

    YT_PAY_JSAPI("163", "远图支付平台JSAPI支付", "JSAPI", PayServiceConstant.YTPAY, "远图支付平台"),

    NET_PAY_JSAPI("164", "全民付JSAPI支付", "JSAPI", PayServiceConstant.NETPAY, "全民付"),

    BS_PAY_JSAPI("165", "创业支付平台JSAPI支付", "JSAPI", PayServiceConstant.BSPAY, "创业支付平台"),

    JKWY_PAY_WAP("166", "宁夏健康无忧支付平台WAP支付", "WAP", PayServiceConstant.JKWYPAY, "宁夏健康无忧支付平台"),
    JKWY_PAY_APP("169", "宁夏健康无忧支付平台APP支付", "APP", PayServiceConstant.JKWYPAY, "宁夏健康无忧支付平台"),

    LIANLIAN_PAY_JSAPI("167", "连连支付JSAPI支付", "JSAPI", PayServiceConstant.LIANLIANPAY, "连连支付"),

    XNUNION_PAY_JSAPI("168", "西宁统一支付平台JSAPI支付", "JSAPI", PayServiceConstant.XNUNIONPAY, "西宁统一支付平台"),

    JYT_PAY_WAP("170", "近医通支付平台WAP支付", "WAP", PayServiceConstant.JYTPAY, "近医通支付平台"),

    //其他支付
    VIRTUAL_ACCOUNT("97", "虚拟账户", "", null, "其他"),
    CITIZEN_CARD("98", "市民卡支付", "", null, "其他"),
    POS("99", "POS消费", "", null, "银联");

    /**
     * 对应支付平台pay_way字段值
     */
    private String code;
    private String name;

    private String payWay;

    private Integer payType;

    private String payTypeName;

    /**
     * @param code
     * @param name
     * @param payWay
     * @param payType
     * @param payTypeName
     */
    PayWayEnum(String code, String name, String payWay, Integer payType, String payTypeName) {
        this.code = code;
        this.name = name;
        this.payWay = payWay;
        this.payType = payType;
        this.payTypeName = payTypeName;
    }

    public static PayWayEnum fromCode(String code) {
        if (code == null || code.trim().equals("")) {
            return null;
        }
        for (PayWayEnum e : PayWayEnum.values()) {
            if (e.getCode().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getPayWay() {
        return payWay;
    }

    public Integer getPayType() {
        return payType;
    }

    public String getPayTypeName() {
        return payTypeName;
    }
}
