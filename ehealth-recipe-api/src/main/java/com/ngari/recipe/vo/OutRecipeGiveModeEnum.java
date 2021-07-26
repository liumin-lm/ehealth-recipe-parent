package com.ngari.recipe.vo;

public enum OutRecipeGiveModeEnum {
    OUTRECIPETOHOS(1, "院内自取"),
    OUTRECIPESENDHOS(2, "医院配送"),
    OUTRECIPESENDTHIRD(3, "第三方配送");

    private Integer status;
    private String name;

    OutRecipeGiveModeEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public static String getName(Integer status){
        OutRecipeGiveModeEnum[] outRecipeStatusEnums = OutRecipeGiveModeEnum.values();
        for (OutRecipeGiveModeEnum outRecipeGiveModeEnum : outRecipeStatusEnums){
            if (outRecipeGiveModeEnum.status.equals(status)){
                return outRecipeGiveModeEnum.getName();
            }
        }
        return "";
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
