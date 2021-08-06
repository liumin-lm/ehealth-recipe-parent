package com.ngari.recipe.recipe.constant;

public enum RecipeTypeEnum {

    /**
     * 处方类型-西药  Western medicine
     */
    RECIPETYPE_WM(1,"西药"),
    /**
     * 处方类型-中成药  chinese patent medicine.
     */
    RECIPETYPE_CPM(2,"中成药"),
    /**
     * 处方类型-中药  traditional Chinese medicine
     */
    RECIPETYPE_TCM(3,"中药"),
    /**
     * 处方类型-膏方 Herbal Paste
     */
    RECIPETYPE_HP(4,"膏方");



    private Integer type;
    private String text;

    RecipeTypeEnum(Integer type, String text) {
        this.type = type;
        this.text = text;
    }

    public Integer getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public static String getRecipeType(Integer type){
        for (RecipeTypeEnum value : RecipeTypeEnum.values()) {
            if (value.getType().equals(type)){
                return value.getText();
            }
        }
        return null;
    }
}
