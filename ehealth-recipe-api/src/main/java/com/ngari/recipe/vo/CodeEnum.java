package com.ngari.recipe.vo;

/**
 * 错误码枚举
 *
 * @author fuzi
 */

public enum CodeEnum {

    SERVICE_SUCCEED(200, "成功", "服务成功"),
    SERVICE_ERROR(609, "系统异常", "提示消息"),
    ;


    private Integer code;
    private String name;
    private String desc;

    CodeEnum(Integer code, String name, String desc) {
        this.code = code;
        this.name = name;
        this.desc = desc;
    }


    public static String getCodeName(Integer code) {
        for (CodeEnum e : CodeEnum.values()) {
            if (e.code.equals(code)) {
                return e.name;
            }
        }
        return "未知";
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}
