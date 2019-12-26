package recipe.drugsenterprise.bean.yd.model;


import recipe.drugsenterprise.bean.yd.utils.GsonUtils;

public class StdInputVo implements JsonAware{

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

    public EncryptMode getEncryptMode() {
        return encryptMode==null?null:EncryptMode.valueOf(encryptMode);
    }

    public void setEncryptMode(String encryptMode) {
        this.encryptMode = encryptMode;
    }

    public String toJSONString() {
        return GsonUtils.toJson(this);
    }

}
