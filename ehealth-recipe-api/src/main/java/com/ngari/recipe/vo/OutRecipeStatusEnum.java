package com.ngari.recipe.vo;

public enum OutRecipeStatusEnum {

    OUTRECIPEREADYPAY(2, "代缴费"),
    OUTRECIPEREADYSEND(3, "待发药"),
    OUTRECIPEHASSEND(6, "已发药"),
    OUTRECIPEHASREFUND(9, "已退费");

    private Integer status;
    private String name;

    OutRecipeStatusEnum(Integer status, String name){
        this.status = status;
        this.name = name;
    }

    public static String getName(Integer status){
        OutRecipeStatusEnum[] values = OutRecipeStatusEnum.values();
        for (OutRecipeStatusEnum outRecipeStatusEnum : values) {
            if (outRecipeStatusEnum.status.equals(status)) {
                return outRecipeStatusEnum.getName();
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
