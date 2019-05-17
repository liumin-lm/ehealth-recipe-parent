package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * @author yinsheng
 * @date 2019\2\28 0028 15:00
 */
@Schema
public class DyfRecipeDTO implements Serializable{

    private static final long serialVersionUID = 1604980563324503439L;

    @Verify(isNotNull = true, desc = "处⽅单信息")
    private PrescriptionParamDTO prescriptionParam;

    @Verify(isNotNull = true, desc = "医院/医⽣信息")
    private DoctorParamDTO doctorParam;

    @Verify(isNotNull = true, desc = "患者信息")
    private PatientParamDTO patientParam;

    @Verify(isNotNull = true, desc = "诊断信息")
    private DiagnosticParamDTO diagnosticParam;

    @Verify(isNotNull = true, desc = "药品信息")
    private List<DrugParamDTO> drugList;

    @Verify(isNotNull = false, desc = "回传信息")
    private List<ChooseDrugParamDTO> chooseDrugList;

    public PrescriptionParamDTO getPrescriptionParam() {
        return prescriptionParam;
    }

    public void setPrescriptionParam(PrescriptionParamDTO prescriptionParam) {
        this.prescriptionParam = prescriptionParam;
    }

    public DoctorParamDTO getDoctorParam() {
        return doctorParam;
    }

    public void setDoctorParam(DoctorParamDTO doctorParam) {
        this.doctorParam = doctorParam;
    }

    public PatientParamDTO getPatientParam() {
        return patientParam;
    }

    public void setPatientParam(PatientParamDTO patientParam) {
        this.patientParam = patientParam;
    }

    public DiagnosticParamDTO getDiagnosticParam() {
        return diagnosticParam;
    }

    public void setDiagnosticParam(DiagnosticParamDTO diagnosticParam) {
        this.diagnosticParam = diagnosticParam;
    }

    public List<DrugParamDTO> getDrugList() {
        return drugList;
    }

    public void setDrugList(List<DrugParamDTO> drugList) {
        this.drugList = drugList;
    }

    public List<ChooseDrugParamDTO> getChooseDrugList() {
        return chooseDrugList;
    }

    public void setChooseDrugList(List<ChooseDrugParamDTO> chooseDrugList) {
        this.chooseDrugList = chooseDrugList;
    }
}
