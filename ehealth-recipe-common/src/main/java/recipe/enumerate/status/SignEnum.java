package recipe.enumerate.status;

/**
 * 签名状态枚举
 *
 * @author fuzi
 */

public enum SignEnum {
    NONE(0, "默认", "未签名"),
    SIGN_STATE_SUBMIT(1, "签名中", ""),
    SIGN_STATE_AUDIT(2, "签名失败", ""),
    SIGN_STATE_ORDER(3, "签名成功", ""),
    ;

    private Integer type;
    private String name;
    private String desc;

    SignEnum(Integer type, String name, String desc) {
        this.type = type;
        this.name = name;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
