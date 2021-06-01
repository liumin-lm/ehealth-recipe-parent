package recipe.medicationguide.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * created by shiyuping on 2019/10/28
 * @author shiyuping
 */
public class DiagnosisInfoDTO implements Serializable {
    private static final long serialVersionUID = -3698369252331227884L;
    /**
     * 诊断代码
     */
    @JsonProperty("DiagnosisCode")
    private String diagnosisCode;
    /**
     * 诊断名称
     */
    @JsonProperty("DiagnosisName")
    private String diagnosisName;

    public String getDiagnosisCode() {
        return diagnosisCode;
    }

    public void setDiagnosisCode(String diagnosisCode) {
        this.diagnosisCode = diagnosisCode;
    }

    public String getDiagnosisName() {
        return diagnosisName;
    }

    public void setDiagnosisName(String diagnosisName) {
        this.diagnosisName = diagnosisName;
    }
}
