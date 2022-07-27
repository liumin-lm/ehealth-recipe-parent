package recipe.enumerate.status;

/**
 * 结算状态
 */
public enum SettleAmountStateEnum {
    NONE_SETTLE(0, "未结算"),
    SETTLE_SUCCESS(1, "结算成功"),
    SETTLE_FAIL(2, "结算失败"),
    NO_NEED(3, "无需结算")
    ;
    SettleAmountStateEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    private Integer type;
    private String name;

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
