package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 支付宝token更新response
 * @version： 1.0
 */
public class ZfbTokenResponse implements Serializable{

    private static final long serialVersionUID = -7109655974850412831L;

    private String msg;

    private String code;

    private String token;

    public ZfbTokenResponse() {
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
