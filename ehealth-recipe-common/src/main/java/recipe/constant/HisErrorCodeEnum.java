package recipe.constant;


/**
 * 对接前置机错误码
 *
 * @author fuzi
 */

public enum HisErrorCodeEnum {

    HIS_SUCCEED(200, "返回数据成功", "前置返回数据成功"),
    HIS_NULL_ERROR(700, "返回数据为空", "前置机返回为null"),
    HIS_CODE_ERROR(701, "返回代码出错", "前置机代码返回出错"),
    HIS_PARAMETER_ERROR(702, "返回出参为空", "前置机出参为null"),
    ;

    private int code;
    private String msg;
    private String desc;

    HisErrorCodeEnum(int code, String msg, String desc) {
        this.code = code;
        this.msg = msg;
        this.desc = desc;
    }


    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
