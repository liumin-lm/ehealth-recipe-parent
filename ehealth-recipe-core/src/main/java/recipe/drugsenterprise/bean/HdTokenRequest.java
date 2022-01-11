package recipe.drugsenterprise.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
/**
* @Description: 华东token请求对象
* @Author: JRK
* @Date: 2019/7/25
*/
public class HdTokenRequest implements Serializable{
    private static final long serialVersionUID = 1186027219917905365L;
    /**
     * 用户端id
     */
    @JsonProperty(value = "client_id")
    private String clientId;
    /**
     * 用户端密码
     */
    @JsonProperty(value = "client_secret")
    private String clientSecret;
    /**
     * 授权的状态
     */
    @JsonProperty(value = "grant_type")
    private String grantType;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }
}