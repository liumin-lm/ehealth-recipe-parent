package recipe.enumerate.status;

public enum OutRecipeStatusEnum {

    OUTRECIPEREADYPAY(2, "代缴费"),
    OUTRECIPEREADYSEND(3, "待发药"),
    OUTRECIPEHASSEND(6, "已发药"),
    OUTRECIPEHASREFUND(9, "已退费");

    private Integer type;
    private String name;

    OutRecipeStatusEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }

    public static String getName(Integer type){
        OutRecipeStatusEnum[] values = OutRecipeStatusEnum.values();
        for (OutRecipeStatusEnum outRecipeStatusEnum : values) {
            if (outRecipeStatusEnum.type.equals(type)) {
                return outRecipeStatusEnum.getName();
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
