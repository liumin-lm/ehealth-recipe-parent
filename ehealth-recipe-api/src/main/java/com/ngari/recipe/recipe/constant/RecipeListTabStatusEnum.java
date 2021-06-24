package com.ngari.recipe.recipe.constant;


/**
 * @description： 处方列表tab类型枚举
 * @author： whf
 * @date： 2021-06-04 10:19
 */
public enum RecipeListTabStatusEnum {
    /**
     * 待处理onready待处理 ongoing进行中 onover已结束
     */
    ON_READY("onready", 1, "待处理"),

    /**
     * 进行中
     */
    ON_GOING("ongoing", 2, "进行中"),

    /**
     * 已完成
     */
    ON_OVER("isover", 3, "已完成"),
    ;

    /**
     * 前端传入文案
     */
    private String text;
    /**
     * type
     */
    private Integer type;

    /**
     * 含义
     */
    private String name;


    RecipeListTabStatusEnum(String text, Integer type, String name) {
        this.text = text;
        this.type = type;
        this.name = name;
    }

    public static RecipeListTabStatusEnum getRecipeListTabStatusEnum(Integer type) {
        for (RecipeListTabStatusEnum e : RecipeListTabStatusEnum.values()) {
            if (e.getType().equals(type)) {
                return e;
            }
        }
        return null;
    }

    public String getText() {
        return text;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
