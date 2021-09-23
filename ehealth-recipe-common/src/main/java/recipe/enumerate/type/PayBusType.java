package recipe.enumerate.type;

/**
 * 支付业务类型
 */
public enum PayBusType {
    RECIPE_BUS_TYPE(1, "recipe"),
    OTHER_BUS_TYPE(2, "Recipeotherfee");

    private Integer type;
    private String name;

    PayBusType(Integer type, String name){
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
