package recipe.enumerate.type;

/**
 * 处方结算维度
 */
public enum OrderSettleDimensionTypeEnum {
    ORDER_DIMENSION(0, "订单维度"),
    RECIPE_DIMENSION(1, "处方维度");

    private Integer type;
    private String name;

    OrderSettleDimensionTypeEnum(Integer type, String name){
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
