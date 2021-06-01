package recipe.medicationguide.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * created by shiyuping on 2019/10/28
 * @author shiyuping
 */
public class DrugUseDTO implements Serializable {
    private static final long serialVersionUID = -6668343352029050053L;
    /**
     * 药品代码
     */
    @JsonProperty("DrugCode")
    private String drugCode;
    /**
     * 药品名称
     */
    @JsonProperty("DrugName")
    private String drugName;
    /**
     * 药品使用方式（用法用量）eg:口服/每次75mg/每日两次
     */
    @JsonProperty("DrugUsed")
    private String drugUsed;

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getDrugUsed() {
        return drugUsed;
    }

    public void setDrugUsed(String drugUsed) {
        this.drugUsed = drugUsed;
    }
}
