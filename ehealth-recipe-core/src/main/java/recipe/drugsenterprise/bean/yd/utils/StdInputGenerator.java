package recipe.drugsenterprise.bean.yd.utils;

import recipe.drugsenterprise.bean.yd.model.EncryptMode;
import recipe.drugsenterprise.bean.yd.model.StdInputVo;
import recipe.drugsenterprise.bean.yd.model.StdParamVo;

import java.security.interfaces.RSAPublicKey;
import java.util.Random;

public class StdInputGenerator {

    /**
     *  获取随机数 (由纯数字组成)
     * @param len
     * @return
     */
    public static String getRandom(int len) {
        StringBuffer sBuffer = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < len; i++) {
            sBuffer.append(random.nextInt(10));
        }
        return sBuffer.toString();
    }

    public static StdInputVo toStdInputVo(String appid, String appsecret, String paramJson, EncryptMode encryptMode, String publicKeyPath1) throws Exception {
        StdInputVo inputVo = new StdInputVo();
        inputVo.setAppid(appid);
        StdParamVo paramVo = new StdParamVo();
        paramVo.setAppid(appid);
        paramVo.setAppkey(appsecret);
        paramVo.setTimestamp(System.currentTimeMillis());
        paramVo.setRandom(Integer.parseInt(getRandom(8)));
        paramVo.setParam(paramJson);
        paramVo.setParamLength(paramJson.length());
        String param = paramVo.toJSONString();
        String cipher = null;
        switch (encryptMode==null? EncryptMode.DESEDE:encryptMode){
            case DESEDE:
                cipher = EncrypterUtils.encode(param);
                break;
            case RSA:
                //外部系统用公钥1加密数据传输，处方平台系统用私钥1解密数据
                RSAPublicKey publicKey = RSAEncryptUtils.loadPublicKeyByFile(publicKeyPath1);
                cipher = RSAEncryptUtils.encryptToHexString(publicKey, param);
                break;
        }
        inputVo.setCipher(cipher);
        inputVo.setSignature(paramVo.toMD5String());
        inputVo.setEncryptMode(encryptMode.name());
        return inputVo;
    }

    public static StdInputVo toStdInputVo(String appid,String appsecret,String paramJson) throws Exception {
        return toStdInputVo(appid,appsecret,paramJson,EncryptMode.DESEDE,null);
    }

}