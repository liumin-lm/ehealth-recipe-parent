package recipe.drugsenterprise.bean;

import java.io.Serializable;
import java.util.List;

/**
 * @author yinsheng
 * @date 2019\10\28 0028 16:34
 */
public class HrPrescr implements Serializable{
    private static final long serialVersionUID = 6168799146274038946L;

    private String PatientId;
    private String PrescrDate;
    private String ImageUri;
    private String PrescrNo;
    private String BuyerMobile;
    private String BuyerId;
    private String DiagnosisResult;
    private String MedicalOrder;
    private String ReviewerName;
    private String Description;
    private String HospitalName;
    private String DepartmentName;
    private String DoctorName;
    private String HospitalId;
    private String DoctorId;
    private String PrescrAmount;
    private List<HrDetail> Details;

    public String getPatientId() {
        return PatientId;
    }

    public void setPatientId(String patientId) {
        PatientId = patientId;
    }

    public String getPrescrDate() {
        return PrescrDate;
    }

    public void setPrescrDate(String prescrDate) {
        PrescrDate = prescrDate;
    }

    public String getImageUri() {
        return ImageUri;
    }

    public void setImageUri(String imageUri) {
        ImageUri = imageUri;
    }

    public String getPrescrNo() {
        return PrescrNo;
    }

    public void setPrescrNo(String prescrNo) {
        PrescrNo = prescrNo;
    }

    public String getBuyerMobile() {
        return BuyerMobile;
    }

    public void setBuyerMobile(String buyerMobile) {
        BuyerMobile = buyerMobile;
    }

    public String getBuyerId() {
        return BuyerId;
    }

    public void setBuyerId(String buyerId) {
        BuyerId = buyerId;
    }

    public String getDiagnosisResult() {
        return DiagnosisResult;
    }

    public void setDiagnosisResult(String diagnosisResult) {
        DiagnosisResult = diagnosisResult;
    }

    public String getMedicalOrder() {
        return MedicalOrder;
    }

    public void setMedicalOrder(String medicalOrder) {
        MedicalOrder = medicalOrder;
    }

    public String getReviewerName() {
        return ReviewerName;
    }

    public void setReviewerName(String reviewerName) {
        ReviewerName = reviewerName;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public String getHospitalName() {
        return HospitalName;
    }

    public void setHospitalName(String hospitalName) {
        HospitalName = hospitalName;
    }

    public String getDepartmentName() {
        return DepartmentName;
    }

    public void setDepartmentName(String departmentName) {
        DepartmentName = departmentName;
    }

    public String getDoctorName() {
        return DoctorName;
    }

    public void setDoctorName(String doctorName) {
        DoctorName = doctorName;
    }

    public String getHospitalId() {
        return HospitalId;
    }

    public void setHospitalId(String hospitalId) {
        HospitalId = hospitalId;
    }

    public String getDoctorId() {
        return DoctorId;
    }

    public void setDoctorId(String doctorId) {
        DoctorId = doctorId;
    }

    public String getPrescrAmount() {
        return PrescrAmount;
    }

    public void setPrescrAmount(String prescrAmount) {
        PrescrAmount = prescrAmount;
    }

    public List<HrDetail> getDetails() {
        return Details;
    }

    public void setDetails(List<HrDetail> details) {
        Details = details;
    }
}
