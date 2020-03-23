package recipe.bussutil.openapi.request.province;

import javax.validation.constraints.NotNull;
import java.util.Date;

public class OrganBussReq implements java.io.Serializable {

    private static final long serialVersionUID = -1332134978582942194L;

    @NotNull
    private String organID;
    @NotNull
    private String  organName;
    @NotNull
    private String subjectCode;
    @NotNull
    private String subjectName;
    private String desc;
    private String note;
    @NotNull
    private Date applyDate;

    @NotNull
    private String deputyDoctorCertID;
    private String deputyDoctorName;
    @NotNull
    private String principalDoctorCertID;
    private String principalDoctorName;
    @NotNull
    private String  unitID;



    public String getUnitID() {
        return unitID;
    }

    public void setUnitID(String unitID) {
        this.unitID = unitID;
    }


    public String getDeputyDoctorCertID() {
        return deputyDoctorCertID;
    }

    public void setDeputyDoctorCertID(String deputyDoctorCertID) {
        this.deputyDoctorCertID = deputyDoctorCertID;
    }

    public String getDeputyDoctorName() {
        return deputyDoctorName;
    }

    public void setDeputyDoctorName(String deputyDoctorName) {
        this.deputyDoctorName = deputyDoctorName;
    }

    public String getPrincipalDoctorCertID() {
        return principalDoctorCertID;
    }

    public void setPrincipalDoctorCertID(String principalDoctorCertID) {
        this.principalDoctorCertID = principalDoctorCertID;
    }

    public String getPrincipalDoctorName() {
        return principalDoctorName;
    }

    public void setPrincipalDoctorName(String principalDoctorName) {
        this.principalDoctorName = principalDoctorName;
    }

    public String getOrganID() {
        return organID;
    }

    public void setOrganID(String organID) {
        this.organID = organID;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getApplyDate() {
        return applyDate;
    }

    public void setApplyDate(Date applyDate) {
        this.applyDate = applyDate;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }
}
