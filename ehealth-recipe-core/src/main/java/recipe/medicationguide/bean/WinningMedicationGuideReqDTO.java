package recipe.medicationguide.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * created by shiyuping on 2019/10/28
 * @author shiyuping
 */
public class WinningMedicationGuideReqDTO implements Serializable {
    private static final long serialVersionUID = -19186028335745558L;
    /**
     * 患者信息数据
     */
    @JsonProperty("PatientInfo")
    private PatientInfoDTO patientInfo;
    /**
     * 医院信息数据
     */
    @JsonProperty("HospInfo")
    private HospInfoDTO hospInfo;
    /**
     * 药品信息数据
     */
    @JsonProperty("DrugUseList")
    private List<DrugUseDTO> drugUseList;
    /**
     * 诊断信息数据
     */
    @JsonProperty("DiagnosisInfoList")
    private List<DiagnosisInfoDTO> diagnosisInfo;
    /**
     * 请求类型
     */
    @JsonProperty("ReqType")
    private Integer reqType;

    public PatientInfoDTO getPatientInfo() {
        return patientInfo;
    }

    public void setPatientInfo(PatientInfoDTO patientInfo) {
        this.patientInfo = patientInfo;
    }

    public HospInfoDTO getHospInfo() {
        return hospInfo;
    }

    public void setHospInfo(HospInfoDTO hospInfo) {
        this.hospInfo = hospInfo;
    }

    public List<DrugUseDTO> getDrugUseList() {
        return drugUseList;
    }

    public void setDrugUseList(List<DrugUseDTO> drugUseList) {
        this.drugUseList = drugUseList;
    }

    public List<DiagnosisInfoDTO> getDiagnosisInfo() {
        return diagnosisInfo;
    }

    public void setDiagnosisInfo(List<DiagnosisInfoDTO> diagnosisInfo) {
        this.diagnosisInfo = diagnosisInfo;
    }

    public Integer getReqType() {
        return reqType;
    }

    public void setReqType(Integer reqType) {
        this.reqType = reqType;
    }
}
