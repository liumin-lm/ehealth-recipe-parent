package com.ngari.recipe.recipe.constant;

import org.apache.commons.collections.CollectionUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @description： 处方支持的购药方式
 * @author： whf
 * @date： 2021-04-26 13:51
 */
public enum RecipeSupportGiveModeEnum {
    /**
     * 到店取药
     */
    SUPPORT_TFDS("supportTFDS", 1, "到店取药"),

    /**
     * showSendToHos 医院配送
     */
    SHOW_SEND_TO_HOS("showSendToHos", 2, "医院配送"),

    /**
     * showSendToEnterprises 药企配送
     */
    SHOW_SEND_TO_ENTERPRISES("showSendToEnterprises", 3, "药企配送"),

    /**
     * supportToHos 到院取药
     */
    SUPPORT_TO_HOS("supportToHos", 4, "到院取药"),

    /**
     * downloadRecipe 下载处方笺
     */
    DOWNLOAD_RECIPE("supportDownload", 5, "下载处方笺"),


    /**
     * supportMedicalPayment 例外支付
     */
    SUPPORT_MEDICAL_PAYMENT("supportMedicalPayment", 6, "例外支付");


    /**
     * 配送文案
     */
    private String text;
    /**
     * 配送状态
     */
    private Integer type;

    private String name;


    RecipeSupportGiveModeEnum(String text, Integer type, String name) {
        this.text = text;
        this.type = type;
        this.name = name;
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

    private static List<String> enterpriseList = Arrays.asList(SHOW_SEND_TO_HOS.text, SHOW_SEND_TO_ENTERPRISES.text, SUPPORT_TFDS.text);

    /**
     * 校验何种类型库存
     *
     * @param configurations 按钮
     * @return
     */
    public static Integer checkFlag(List<String> configurations) {
        if (CollectionUtils.isEmpty(configurations)) {
            return DrugStockCheckEnum.NO_CHECK_STOCK.getType();
        }
        int hospital = DrugStockCheckEnum.NO_CHECK_STOCK.getType();
        int enterprise = DrugStockCheckEnum.NO_CHECK_STOCK.getType();
        for (String a : configurations) {
            if (SUPPORT_TO_HOS.getText().equals(a)) {
                hospital = DrugStockCheckEnum.HOS_CHECK_STOCK.getType();
            }
            if (enterpriseList.contains(a)) {
                enterprise = DrugStockCheckEnum.ENT_CHECK_STOCK.getType();
            }
        }
        return hospital + enterprise;
    }
}