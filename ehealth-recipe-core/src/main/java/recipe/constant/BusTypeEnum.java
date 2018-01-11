package recipe.constant;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 */
public enum BusTypeEnum {
    /**
     * 说明：支付业务类型与运营维护的支付业务类型并不是完全一致，若要添加新的业务类型需与表base_defaultpaytarget中类型对应方可使用
     * 微信支付业务类型：
     * 支付业务类型     CODE           对应运营平台的businessTypeKey
     * ***************************************************************
     * 转诊         transfer，
     * 咨询         consult，           consult
     * 处方         recipe，            recipe
     * 预约挂号     appoint，           realtimeappoint
     * 门诊缴费     outpatient，        outpatient
     * 住院预交     prepay，            inhospital
     * 签约        sign                sign
     * 检查        check               check
     */
    TRANSFER(1, "transfer", "转诊", "转诊业务", "specialappoint", "tsf", "tsf-ref"),
    MEETCLINIC(2, "meetclinic", "会诊", "会诊业务", "", "mtc", "mtc-ref"),
    CONSULT(3, "consult", "咨询", "图文咨询", "consult", "cst", "cst-ref"),
    APPOINT(4, "appoint", "预约", "预约挂号", "realtimeappoint", "app", "app-ref"),
    CHECK(5, "check", "检查", "检验检查", "check", "chk", "chk-ref"),
    RECIPE(6, "recipe", "处方", "电子处方", "recipe", "rcp", "rcp-ref"),
    SIGN(7, "sign", "签约", "签约", "sign", "sgn", "sgn-ref"),
    OUTPATIENT(8, "outpatient", "门诊缴费", "门诊缴费", "outpatient", "out", "out-ref"),
    PREPAY(9, "prepay", "住院预交", "住院缴费", "inhospital", "pre", "pre-ref"),
    APPOINTPAY(10, "appointpay", "预约支付", "预约挂号", "appoint", "app", "app-ref"),
    APPOINTCLOUD(11, "appointcloud", "预约云门诊", "预约云门诊", "appointcloud", "apc", "apc-ref"),
    EMERGENCY(12, "emergency", "红色呼救", "红色呼救", "redcall", "emg", "emg-ref"),
    MINDGIFT(20, "mindgift", "心意", "心意", "mindgift", "mgt", "mgt-reno"),
    LIVECOURSE(21, "livecourse", "课程直播", "课程直播", "livecourse", "lvcs", "lvcs-ref");

    public static final String DT_LONG = "yyyyMMddHHmmssSSS";
    /**
     * 业务类型id 暂时未使用
     **/
    private int id;
    /**
     * 业务类型code 即支付接口使用的busType
     **/
    private String code;
    /**
     * 业务类型名称 用于支付过程业务提示日志等
     **/
    private String name;
    /**
     * 业务类型描述 用于跨公众号支付页的业务名称字段值
     **/
    private String desc;
    /**
     * 运营平台配置的标识值 与code值一一对应
     **/
    private String osBusinessTypeKey;
    /**
     * 业务支付申请单前缀
     **/
    private String applyPrefix;
    /**
     * 业务退款申请单前缀
     **/
    private String refundPrefix;

    BusTypeEnum(int id, String code, String name, String desc, String osBusinessTypeKey, String applyPrefix, String refundPrefix) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.desc = desc;
        this.osBusinessTypeKey = osBusinessTypeKey;
        this.applyPrefix = applyPrefix;
        this.refundPrefix = refundPrefix;
    }

    public static BusTypeEnum fromCode(String code) {
        for (BusTypeEnum e : BusTypeEnum.values()) {
            if (e.getCode().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return null;
    }

    public static BusTypeEnum fromId(int id) {
        for (BusTypeEnum e : BusTypeEnum.values()) {
            if (e.getId() == id) {
                return e;
            }
        }
        return null;
    }

    public static BusTypeEnum fromPrefix(String stringWithPrefix) {
        for (BusTypeEnum e : BusTypeEnum.values()) {
            if (stringWithPrefix.startsWith(e.applyPrefix()) || stringWithPrefix.startsWith(e.refundPrefix())) {
                return e;
            }
        }
        return null;
    }

    public int getId() {
        return this.id;
    }

    public String getCode() {
        return this.code;
    }

    public String getName() {
        return this.name;
    }

    public String getDesc() {
        return this.desc;
    }

    public String getOsBusinessTypeKey() {
        return this.osBusinessTypeKey;
    }

    /**
     * 获取申请支付前缀
     *
     * @return
     */
    public String applyPrefix() {
        return this.applyPrefix;
    }

    /**
     * 获取退款前缀
     *
     * @return
     */
    public String refundPrefix() {
        return this.refundPrefix;
    }

    /**
     * 返回新的支付申请单号
     *
     * @return
     */
    public String getApplyNo() {
        return this.applyPrefix + getSuffix();
    }

    /**
     * 返回新的退款申请单号
     *
     * @return
     */
    public String getRefundNo() {
        return this.refundPrefix + getSuffix();
    }

    /**
     * 》内部方法
     */
    public static String getSuffix() {
        return System.currentTimeMillis() + getTwo();
    }

    /**
     * 》内部方法
     */
    protected static String getCurrentTime() {
        Date date = new Date();
        DateFormat df = new SimpleDateFormat(DT_LONG);
        return df.format(date);
    }

    /**
     * 》内部方法
     */
    protected static String getTwo() {
        return String.valueOf((int) (Math.random() * 100 + 100)).substring(1);
    }

}
