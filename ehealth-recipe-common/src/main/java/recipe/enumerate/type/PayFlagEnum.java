package recipe.enumerate.type;

/**
 * 支付标志
 */
public enum PayFlagEnum {
    NOPAY(0, "未支付"),
    PAYED(1, "已支付"),
    REFUND_SUCCESS(3, "退费成功"),
    REFUND_FAIL(4, "退费失败");

    private Integer type;
    private String name;

    PayFlagEnum(Integer type, String name) {
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