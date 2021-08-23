package recipe.enumerate.type;

public enum OutRecipeGiveModeEnum {
    OUTRECIPETOHOS(1, "院内自取"),
    OUTRECIPESENDHOS(2, "医院配送"),
    OUTRECIPESENDTHIRD(3, "第三方配送");

    private Integer type;
    private String name;

    OutRecipeGiveModeEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public static String getName(Integer status){
        OutRecipeGiveModeEnum[] outRecipeStatusEnums = OutRecipeGiveModeEnum.values();
        for (OutRecipeGiveModeEnum outRecipeGiveModeEnum : outRecipeStatusEnums){
            if (outRecipeGiveModeEnum.type.equals(status)){
                return outRecipeGiveModeEnum.getName();
            }
        }
        return "";
    }

    public Integer getStatus() {
        return type;
    }

    public String getName() {
        return name;
    }
}
