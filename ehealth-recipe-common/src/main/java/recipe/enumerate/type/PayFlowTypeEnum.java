package recipe.enumerate.type;

/**
 * 支付流水类型
 *
 * @author yinsheng
 */
public enum PayFlowTypeEnum {
    RECIPE_FLOW(1, "处方药品支付流水"),
    RECIPE_AUDIT(2, "审方或快递支付流水");

    private Integer type;
    private String name;

    PayFlowTypeEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }
}
