package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;
import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\2\28 0028 15:33
 */
@Schema
public class DiagnosticParamDTO implements Serializable{

    private static final long serialVersionUID = -3211504926926481132L;

    @Verify(isNotNull = true, desc = "患者主诉", maxLength = 100)
    private String complaints;

    @Verify(isNotNull = true, desc = "诊断", maxLength = 100)
    private String diagnosis;

    @Verify(isNotNull = false, desc = "疾病", maxLength = 100)
    private String disease;

    public String getComplaints() {
        return complaints;
    }

    public void setComplaints(String complaints) {
        this.complaints = complaints;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getDisease() {
        return disease;
    }

    public void setDisease(String disease) {
        this.disease = disease;
    }
}
