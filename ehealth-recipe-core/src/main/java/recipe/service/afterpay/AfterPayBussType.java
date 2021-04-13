package recipe.service.afterpay;

/**
 * @author yinsheng
 * @date 2021\4\12 0012 17:09
 */
public enum AfterPayBussType {
    AFTER_PAY_BUSS_HEALTH_CARD("healthCard", "健康卡业务"),
    AFTER_PAY_BUSS_KEEP_ACCOUNT("keepAccount", "记账业务"),
    AFTER_PAY_BUSS_PAY_SEND_MSG("paySendMsg", "支付消息发送业务"),
    AFTER_PAY_BUSS_LOGISTICS_ORDER("logisticsOnlineOrder", "物流自动下单");

    private String name;
    private String desc;

    AfterPayBussType(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
