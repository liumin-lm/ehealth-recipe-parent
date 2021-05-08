package com.ngari.recipe.recipe.constant;

/**
 * @description： 处方支持的购药方式
 * @author： whf
 * @date： 2021-04-26 13:51
 */
public enum RecipeSupportGiveModeEnum {
    /**
     * 到店取药
     */
    SUPPORT_TFDS("supportTFDS",1),

    /**
     * showSendToHos 医院配送
     */
    SHOW_SEND_TO_HOS("showSendToHos",2),

    /**
     * showSendToEnterprises 药企配送
     */
    SHOW_SEND_TO_ENTERPRISES("showSendToEnterprises",3),

    /**
     * supportToHos 到院取药
     */
    SUPPORT_TO_HOS("supportToHos",4),

    /**
     * downloadRecipe 下载处方笺
     */
    DOWNLOAD_RECIPE("supportDownload",5),

    ;


    /**
     * 配送文案
     */
    private String text;
    /**
     * 配送状态
     */
    private Integer type;

    RecipeSupportGiveModeEnum(String text, Integer type) {
        this.text = text;
        this.type = type;
    }
    public static RecipeSupportGiveModeEnum getRecipeSupportGiveModeEnum(Integer type) {
        for (RecipeSupportGiveModeEnum e : RecipeSupportGiveModeEnum.values()) {
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

}
