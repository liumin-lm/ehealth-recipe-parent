package recipe.bussutil.openapi.request.province;

import javax.validation.constraints.NotNull;
import java.util.Date;

public class DoctorBussReq implements java.io.Serializable {

    private static final long serialVersionUID = -5031848242367781462L;

    @NotNull
    private String organID;
    @NotNull
    private String  organName;
    private String doctorID;
    private String doctorName;
    @NotNull
    private String doctorCertID;
    private String doctorWorkType;
    @NotNull
    private String businessType;
    @NotNull
    private Date applyDate;
    @NotNull
    private String  unitID;



    public String getUnitID() {
        return unitID;
    }

    public void setUnitID(String unitID) {
        this.unitID = unitID;
    }

    public String getOrganID() {
        return organID;
    }

    public void setOrganID(String organID) {
        this.organID = organID;
    }

    public String getDoctorID() {
        return doctorID;
    }

    public void setDoctorID(String doctorID) {
        this.doctorID = doctorID;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDoctorCertID() {
        return doctorCertID;
    }

    public void setDoctorCertID(String doctorCertID) {
        this.doctorCertID = doctorCertID;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
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

    public String getDoctorWorkType() {
        return doctorWorkType;
    }

    public void setDoctorWorkType(String doctorWorkType) {
        this.doctorWorkType = doctorWorkType;
    }
}
