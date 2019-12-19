package recipe.drugsenterprise.bean.yd.model;


import recipe.drugsenterprise.bean.yd.utils.EncrypterUtils;
import recipe.drugsenterprise.bean.yd.utils.GsonUtils;
import recipe.drugsenterprise.bean.yd.utils.Md5Utils;
import recipe.drugsenterprise.bean.yd.utils.RSAEncryptUtils;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class StdOutputVo implements JsonAware {

    private Integer status;
    private String errMsg;
    private String cipher;
    private String signature;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
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

    public static StdOutputVo fromJson(String json){
        return GsonUtils.parseJson(json,StdOutputVo.class);
    }


    public String getPlainText(){
        return decodeStdOutput(null,null);
    }

    /**
     * 获取解密的明文返回数据
     * @param encryptMode 加密模式
     * @param privateKey2Path 处方平台系统用公钥2加密数据传输，外部系统用私钥2解密数据
     * @return
     */
    public String decodeStdOutput(EncryptMode encryptMode,String privateKey2Path){
        try {
            String plainText = null;
            if(this.getCipher() == null || "".equals(this.getCipher())){
            }else{
                switch (encryptMode == null?EncryptMode.DESEDE:encryptMode){
                    case RSA:
                        //处方平台系统用公钥2加密数据传输，外部系统用私钥2解密数据
                        RSAPrivateKey privateKey = RSAEncryptUtils.loadPrivateKeyByFile(privateKey2Path);
                        plainText = RSAEncryptUtils.decryptFromHexString(privateKey,cipher);
                        break;
                    case DESEDE:
                        plainText = EncrypterUtils.decode(this.getCipher());
                        break;
                }
            }
            return plainText;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 加密接口的出参数据
     * @param resultJson 返回值json
     * @param encryptMode 加密模式
     * @param publicKey2Path 处方平台系统用公钥2加密数据传输，外部系统用私钥2解密数据
     */
    public void encodeStdOutput(String resultJson,EncryptMode encryptMode,String publicKey2Path){
        try {
            if(resultJson != null && !"".equals(resultJson)){
                switch (encryptMode == null?EncryptMode.DESEDE:encryptMode){
                    case RSA:
                        RSAPublicKey publicKey = RSAEncryptUtils.loadPublicKeyByFile(publicKey2Path);
                        //RSA公钥加密
                        String cipher = RSAEncryptUtils.encryptToHexString(publicKey, resultJson);
                        this.setCipher(cipher);
                        break;
                    case DESEDE:
                        this.setCipher(EncrypterUtils.encode(resultJson));
                        break;
                    default:break;
                }
                this.setSignature(Md5Utils.crypt(resultJson));
            }
        } catch (Exception e) {
        }
    }

    public String toJSONString() {
        return GsonUtils.toJson(this);
    }
}
