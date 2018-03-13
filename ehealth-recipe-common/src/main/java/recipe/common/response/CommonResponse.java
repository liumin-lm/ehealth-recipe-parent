package recipe.common.response;

/**
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2018/3/8
 */
public class CommonResponse {

    protected String code;

    protected String msg;


    public CommonResponse() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "CommonResponse{" +
                "code='" + code + '\'' +
                ", msg='" + msg + '\'' +
                '}';
    }
}
