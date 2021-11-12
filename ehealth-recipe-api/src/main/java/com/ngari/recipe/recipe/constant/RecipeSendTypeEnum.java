package com.ngari.recipe.recipe.constant;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum RecipeSendTypeEnum {
    NO_PAY("药企配送", 2),
    ALRAEDY_PAY("医院配送", 1);

    private String sendText;
    private Integer sendType;
    private static final Map<Integer, String> map = (Map) Arrays.stream(values()).collect(Collectors.toMap(RecipeSendTypeEnum::getSendType, RecipeSendTypeEnum::getSendText));

    private RecipeSendTypeEnum(String sendText, Integer sendType) {
        this.sendText = sendText;
        this.sendType = sendType;
    }

    public static String getSendText(Integer sendType) {
        return (String)map.get(sendType);
    }

    public String getSendText() {
        return this.sendText;
    }

    public Integer getSendType() {
        return this.sendType;
    }
}
