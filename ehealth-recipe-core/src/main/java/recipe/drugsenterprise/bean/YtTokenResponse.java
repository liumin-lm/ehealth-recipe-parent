package recipe.drugsenterprise.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
* @Description: 英特token响应对象
* @Author: JRK
* @Date: 2019/7/8
*/
public class YtTokenResponse implements Serializable {
    private static final long serialVersionUID = 140828391356314530L;
    /**
     * 新的访问token
     */
    @JsonProperty(value = "TOKEN")
    private String TOKEN;
    /**
     * token的过期时间
     */
    @JsonProperty(value = "EXPIRETIME")
    private String EXPIRETIME;

    public String getTOKEN() {
        return TOKEN;
    }

    public void setTOKEN(String TOKEN) {
        this.TOKEN = TOKEN;
    }

    public String getEXPIRETIME() {
        return EXPIRETIME;
    }

    public void setEXPIRETIME(String EXPIRETIME) {
        this.EXPIRETIME = EXPIRETIME;
    }
}