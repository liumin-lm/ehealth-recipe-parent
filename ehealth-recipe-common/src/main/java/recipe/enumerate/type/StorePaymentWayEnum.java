package recipe.enumerate.type;

/**
 * 到店取药收款方式  0 货到付款 1 在线支付
 */
public enum StorePaymentWayEnum {

    STORE_PAYMENT_WAY_OFFLINE(2, "货到付款"),
    STORE_PAYMENT_WAY_ONLINE(1, "在线支付");
    private Integer type;
    private String name;

    StorePaymentWayEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
