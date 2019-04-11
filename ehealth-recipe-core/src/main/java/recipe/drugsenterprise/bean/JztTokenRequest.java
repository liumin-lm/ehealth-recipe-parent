package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
 * @author： yinsheng
 * @date： 2019/3/19
 * @description： 九州通token请求
 * @version： 1.0
 */
public class JztTokenRequest implements Serializable{

    private static final long serialVersionUID = -5881870011125289283L;

    private String app_id;

    private String app_key;

    private String nonce;

    private String signature;

    private String timestamp;

    public JztTokenRequest(){}

    public String getApp_id() {
        return app_id;
    }

    public void setApp_id(String app_id) {
        this.app_id = app_id;
    }

    public String getApp_key() {
        return app_key;
    }

    public void setApp_key(String app_key) {
        this.app_key = app_key;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
