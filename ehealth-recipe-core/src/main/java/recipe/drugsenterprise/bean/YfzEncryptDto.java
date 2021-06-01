package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * @Description: 对接上海六院易复诊数据加密中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */
@Schema
public class YfzEncryptDto implements Serializable {
    /**
     * key
     */
    private String key;
    /**
     * 需要加密的参数
     */
    private String originaldata;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getOriginaldata() {
        return originaldata;
    }

    public void setOriginaldata(String originaldata) {
        this.originaldata = originaldata;
    }
}
