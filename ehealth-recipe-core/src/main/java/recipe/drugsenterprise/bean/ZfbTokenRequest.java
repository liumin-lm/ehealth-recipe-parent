package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 支付宝token请求
 * @version： 1.0
 */
public class ZfbTokenRequest implements Serializable{

    private static final long serialVersionUID = 2811589964070932506L;

    private String appid;

    private String sign;

    public ZfbTokenRequest(){}

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}
