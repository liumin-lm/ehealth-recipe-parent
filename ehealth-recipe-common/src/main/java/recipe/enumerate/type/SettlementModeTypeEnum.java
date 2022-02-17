package recipe.enumerate.type;

/**
 * 药品价格结算类型
 *
 * @author yinsheng
 */
public enum SettlementModeTypeEnum {
    SETTLEMENT_MODE_ENT(0, "药企价格"),
    SETTLEMENT_MODE_HOS(1, "医院价格");

    private Integer type;
    private String name;

    SettlementModeTypeEnum (Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
