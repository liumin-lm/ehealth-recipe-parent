package recipe.enumerate.type;

/**
 * 门诊处方的类型
 * @author yinsheng
 * @date 2021\7\29 0029 16:21
 */
public enum OutRecipeRecipeTypeEnum {
    RECIPE_TYPE_XY(1, "西药方"),
    RECIPE_TYPE_ZCY(2, "成药方"),
    RECIPE_TYPE_CY(3, "草药方");

    OutRecipeRecipeTypeEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }

    public static String getName(Integer type){
        OutRecipeRecipeTypeEnum[] outRecipeRecipeTypeEnums = OutRecipeRecipeTypeEnum.values();
        for (OutRecipeRecipeTypeEnum outRecipeRecipeTypeEnum : outRecipeRecipeTypeEnums) {
            if (outRecipeRecipeTypeEnum.type.equals(type)) {
                return outRecipeRecipeTypeEnum.getName();
            }
        }
        return "";
    }

    private Integer type;
    private String name;

    public Integer getRecipeType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
