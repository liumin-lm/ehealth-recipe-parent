package recipe.drugsenterprise.bean.yd.model;

import recipe.drugsenterprise.bean.yd.utils.GsonUtils;
import recipe.drugsenterprise.bean.yd.utils.Md5Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StdParamVo implements MD5Aware,JsonAware {

    private String appid;
    private String appkey;
    private String param;
    private Integer paramLength;
    private Long timestamp;
    private Integer random;

    public static Builder builder() {
        return new Builder();
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getAppid() {
        return this.appid;
    }

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }

    public String getAppkey() {
        return this.appkey;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getParam() {
        return this.param;
    }

    public void setParamLength(Integer paramLength) {
        this.paramLength = paramLength;
    }

    public Integer getParamLength() {
        return this.paramLength;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setRandom(Integer random) {
        this.random = random;
    }

    public Integer getRandom() {
        return this.random;
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

    public String toJSONString() {
        return GsonUtils.toJson(this);
    }

    public static class Builder {
        private StdParamVo instance;

        private Builder() {
            instance = new StdParamVo();
        }

        public Builder setAppid(String appid) {
            this.instance.setAppid(appid);
            return this;
        }

        public Builder setAppkey(String appkey) {
            this.instance.setAppkey(appkey);
            return this;
        }

        public Builder setParam(String param) {
            this.instance.setParam(param);
            return this;
        }

        public Builder setParamLength(Integer paramLength) {
            this.instance.setParamLength(paramLength);
            return this;
        }

        public Builder setTimestamp(Long timestamp) {
            this.instance.setTimestamp(timestamp);
            return this;
        }

        public Builder setRandom(Integer random) {
            this.instance.setRandom(random);
            return this;
        }

        public StdParamVo build() {
            return this.instance;
        }
    }


}
