package com.ngari.recipe.recipe.model;


import com.ngari.patient.dto.PatientDTO;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
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
@Schema
public class PatientRecipeDTO implements Serializable {

    private static final long serialVersionUID = -1040081335924874386L;

    private int recipeId;

    private String recordType;

    private Integer recordId;

    private Integer organId;

    private String recordCode;

    private String mpiId;

    private String patientName;

    private String photo;

    private String organDiseaseName;

    private Date signDate;

    private String doctorName;

    private String departName;

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

    private String recipeMode;

    private Integer giveMode;

    private String signFile;

    private List<RecipeDetailBean> recipeDetail;

    private RecipeExtendBean recipeExtend;

    /**
     * 药师签名的处方PDF
     */
    private String chemistSignFile;

    /**
     * 处方的取药窗口
     */
    private String getDrugWindow;

    private Integer payFlag;//支付标志 0未支付，1已支付，2退款中，3退款成功，4支付失败

    private boolean isHiddenRecipeDetail;//是否隐方

    private String recipeCode;

    /**订单详情页用到*/
    private RecipeBean recipe;
    /**订单详情页用到*/
    private PatientDTO patient;

    private String qrName;

    public boolean getIsHiddenRecipeDetail() {
        return isHiddenRecipeDetail;
    }

    public void setIsHiddenRecipeDetail(boolean hiddenRecipeDetail) {
        isHiddenRecipeDetail = hiddenRecipeDetail;
    }

    public Integer getPayFlag() {
        return payFlag;
    }

    public void setPayFlag(Integer payFlag) {
        this.payFlag = payFlag;
    }

    public PatientRecipeDTO() {
    }

    public String getChemistSignFile() {
        return chemistSignFile;
    }

    public void setChemistSignFile(String chemistSignFile) {
        this.chemistSignFile = chemistSignFile;
    }

    public String getGetDrugWindow() {
        return getDrugWindow;
    }

    public void setGetDrugWindow(String getDrugWindow) {
        this.getDrugWindow = getDrugWindow;
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

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
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

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
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

    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }

    public Integer getPayMode() {
        return payMode;
    }

    public void setPayMode(Integer payMode) {
        this.payMode = payMode;
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

    public String getRecipeMode() {
        return recipeMode;
    }

    public void setRecipeMode(String recipeMode) {
        this.recipeMode = recipeMode;
    }

    public List<RecipeDetailBean> getRecipeDetail() {
        return recipeDetail;
    }

    public void setRecipeDetail(List<RecipeDetailBean> recipeDetail) {
        this.recipeDetail = recipeDetail;
    }

    public Integer getGiveMode() {
        return giveMode;
    }

    public void setGiveMode(Integer giveMode) {
        this.giveMode = giveMode;
    }

    public String getSignFile() {
        return signFile;
    }

    public void setSignFile(String signFile) {
        this.signFile = signFile;
    }

    public RecipeExtendBean getRecipeExtend() {
        return recipeExtend;
    }

    public void setRecipeExtend(RecipeExtendBean recipeExtendBean) {
        this.recipeExtend = recipeExtendBean;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDepartName() {
        return departName;
    }

    public void setDepartName(String departName) {
        this.departName = departName;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public RecipeBean getRecipe() {
        return recipe;
    }

    public void setRecipe(RecipeBean recipe) {
        this.recipe = recipe;
    }

    public PatientDTO getPatient() {
        return patient;
    }

    public void setPatient(PatientDTO patient) {
        this.patient = patient;
    }

    public String getQrName() {
        return qrName;
    }

    public void setQrName(String qrName) {
        this.qrName = qrName;
    }
}
