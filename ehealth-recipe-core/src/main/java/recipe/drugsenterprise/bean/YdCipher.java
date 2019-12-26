package recipe.drugsenterprise.bean;

import recipe.util.Md5Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yinsheng
 * @date 2019\12\16 0016 16:31
 */
public class YdCipher implements Serializable{
    private static final long serialVersionUID = -8003078090739520011L;
    private String appid;
    private String appkey;
    private String param;
    private String paramLength;
    private String timestamp;
    private String random;

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getAppkey() {
        return appkey;
    }

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getParamLength() {
        return paramLength;
    }

    public void setParamLength(String paramLength) {
        this.paramLength = paramLength;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getRandom() {
        return random;
    }

    public void setRandom(String random) {
        this.random = random;
    }

    public String toMD5String() {
        //字典序排列，生成md5特征值
        List<String> objects = new ArrayList<String>();
        objects.add(getAppid());
        objects.add(getAppkey());
        objects.add(getParam());
        objects.add(getParamLength()+"");
        objects.add(getRandom()+"");
        objects.add(getTimestamp()+"");
        Collections.sort(objects);
        String str = "";
        for (String s1 : objects) {
            str+=s1+",";
        }
        return Md5Utils.crypt(str);
    }
}
