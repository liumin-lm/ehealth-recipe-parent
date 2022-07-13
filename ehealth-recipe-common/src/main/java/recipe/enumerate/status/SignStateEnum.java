package recipe.enumerate.status;

/**
 * 药师签名状态
 */
public enum SignStateEnum {
    NONE(0, "默认（未签名）"),
    SIGN_SIGNING(1, "签名中"),
    SIGN_FAIL(2, "签名失败"),
    SIGN_SUC(3, "签名成功")
    ;
    SignStateEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }
    private Integer type;
    private String name;

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据类型 获取枚举类型
     *
     * @param type
     * @return
     */
    public static SignStateEnum getSignStateEnum(Integer type) {
        for (SignStateEnum e : SignStateEnum.values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return NONE;
    }
}
