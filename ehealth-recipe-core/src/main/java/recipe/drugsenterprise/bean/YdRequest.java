package recipe.drugsenterprise.bean;

/**
 * @author yinsheng
 * @date 2019\12\16 0016 16:28
 */
public class YdRequest {
    private String appid;
    private String cipher;
    private String signature;
    private String encryptMode;

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getEncryptMode() {
        return encryptMode;
    }

    public void setEncryptMode(String encryptMode) {
        this.encryptMode = encryptMode;
    }
}
