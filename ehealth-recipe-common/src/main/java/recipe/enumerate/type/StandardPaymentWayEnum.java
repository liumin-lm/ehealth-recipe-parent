package recipe.enumerate.type;

/**
 * 标准的收款方式  1 在线支付 2 货到付款
 */
public enum StandardPaymentWayEnum {
    PAYMENT_WAY_ONLINE(1, "在线支付"),
    PAYMENT_WAY_COD(2, "货到付款");

    private Integer type;
    private String name;

    StandardPaymentWayEnum(Integer type, String name){
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
