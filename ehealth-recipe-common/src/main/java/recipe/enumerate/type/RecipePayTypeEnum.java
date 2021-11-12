package recipe.enumerate.type;

/**
 * 支付类型
 */
public enum RecipePayTypeEnum {

    NL_PAY(1, "平台支付"),
    WN_PAY(2, "卫宁支付"),
    SY_PAY(3, "邵逸夫支付");

    private Integer type;
    private String name;

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    RecipePayTypeEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }
}
