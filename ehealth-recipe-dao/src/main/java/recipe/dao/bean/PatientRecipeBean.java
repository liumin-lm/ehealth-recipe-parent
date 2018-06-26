package recipe.dao.bean;


import com.ngari.recipe.entity.Recipedetail;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 患者端处方单对象
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2016/5/12.
 */
public class PatientRecipeBean {

    private int recipeId;

    private String recordType;

    private Integer recordId;

    private String recordCode;

    private String mpiId;

    private String patientName;

    private Integer photo;

    private String organDiseaseName;

    private Date signDate;

    private BigDecimal totalMoney;

    private String statusText;

    private Integer statusCode;

    private String patientSex;

    private String recipeSurplusHours;

    private Integer couponId;

    private Integer medicalPayFlag;

    private Integer recipeType;

    private Integer payMode;

    private boolean checkEnterprise;

    private String logisticsCompany;

    private String trackingNumber;

    private List<Recipedetail> recipeDetail;

    private List<PatientRecipeBean> recipeList;

    public PatientRecipeBean() {
    }

    public int getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }

    public String getRecordCode() {
        return recordCode;
    }

    public void setRecordCode(String recordCode) {
        this.recordCode = recordCode;
    }

    public String getMpiId() {
        return mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Integer getPhoto() {
        return photo;
    }

    public void setPhoto(Integer photo) {
        this.photo = photo;
    }

    public String getOrganDiseaseName() {
        return organDiseaseName;
    }

    public void setOrganDiseaseName(String organDiseaseName) {
        this.organDiseaseName = organDiseaseName;
    }

    public Date getSignDate() {
        return signDate;
    }

    public void setSignDate(Date signDate) {
        this.signDate = signDate;
    }

    public BigDecimal getTotalMoney() {
        return totalMoney;
    }

    public void setTotalMoney(BigDecimal totalMoney) {
        this.totalMoney = totalMoney;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    public String getRecipeSurplusHours() {
        return recipeSurplusHours;
    }

    public void setRecipeSurplusHours(String recipeSurplusHours) {
        this.recipeSurplusHours = recipeSurplusHours;
    }

    public List<PatientRecipeBean> getRecipeList() {
        return recipeList;
    }

    public void setRecipeList(List<PatientRecipeBean> recipeList) {
        this.recipeList = recipeList;
    }

    public List<Recipedetail> getRecipeDetail() {
        return recipeDetail;
    }

    public void setRecipeDetail(List<Recipedetail> recipeDetail) {
        this.recipeDetail = recipeDetail;
    }

    public Integer getCouponId() {
        return couponId;
    }

    public void setCouponId(Integer couponId) {
        this.couponId = couponId;
    }

    public Integer getMedicalPayFlag() {
        return medicalPayFlag;
    }

    public void setMedicalPayFlag(Integer medicalPayFlag) {
        this.medicalPayFlag = medicalPayFlag;
    }

    public Integer getPayMode() {
        return payMode;
    }

    public void setPayMode(Integer payMode) {
        this.payMode = payMode;
    }

    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }

    public boolean isCheckEnterprise() {
        return checkEnterprise;
    }

    public void setCheckEnterprise(boolean checkEnterprise) {
        this.checkEnterprise = checkEnterprise;
    }

    public String getLogisticsCompany() {
        return logisticsCompany;
    }

    public void setLogisticsCompany(String logisticsCompany) {
        this.logisticsCompany = logisticsCompany;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
}
