package recipe.drugsenterprise.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
* @Description: 华东token响应对象
* @Author: JRK
* @Date: 2019/7/24
*/
public class HdTokenResponse implements Serializable {
    private static final long serialVersionUID = 330634181500230584L;
    /**
     * 新的访问access_token
     */
    @JsonProperty(value = "access_token")
    private String accessToken;
    /**
     * token的过期时间
     */
    @JsonProperty(value = "expire_in")
    private String expireIn;
    /**
     * [bearer]固定
     */
    @JsonProperty(value = "token_type")
    private String tokenType;
    /**
     * [all]固定
     */
    @JsonProperty(value = "scope")
    private String scope;

    public String getExpireIn() {
        return expireIn;
    }

    public void setExpireIn(String expireIn) {
        this.expireIn = expireIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}