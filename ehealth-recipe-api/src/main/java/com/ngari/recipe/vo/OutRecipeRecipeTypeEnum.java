package com.ngari.recipe.vo;

/**
 * 门诊处方的类型
 * @author yinsheng
 * @date 2021\7\29 0029 16:21
 */
public enum OutRecipeRecipeTypeEnum {
    RECIPE_TYPE_XY(1, "西药方"),
    RECIPE_TYPE_ZCY(2, "成药方"),
    RECIPE_TYPE_CY(3, "草药方");

    OutRecipeRecipeTypeEnum(Integer recipeType, String name){
        this.recipeType = recipeType;
        this.name = name;
    }

    public static String getName(Integer recipeType){
        OutRecipeRecipeTypeEnum[] outRecipeRecipeTypeEnums = OutRecipeRecipeTypeEnum.values();
        for (OutRecipeRecipeTypeEnum outRecipeRecipeTypeEnum : outRecipeRecipeTypeEnums) {
            if (outRecipeRecipeTypeEnum.recipeType.equals(recipeType)) {
                return outRecipeRecipeTypeEnum.getName();
            }
        }
        return "";
    }

    private Integer recipeType;
    private String name;

    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
