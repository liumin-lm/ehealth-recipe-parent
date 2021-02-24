package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
* @Description: PatientTabStatusRecipeDTO 类（或接口）是患者端tab下处方单对象
* @Author: JRK
* @Date: 2019/8/27
*/
@Schema
public class PatientTabStatusRecipeDTO implements Serializable {

    private static final long serialVersionUID = -1040081335924874386L;

    private int recipeId;

    private String recordType;

    private Integer recordId;

    private Integer organId;

    private String recordCode;

    private String mpiId;
    @Desensitizations(
            type = DesensitizationsType.NAME
    )
    private String patientName;

    private String photo;

    private String organDiseaseName;

    private String doctorName;

    private String departName;

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

    private String recipeMode;

    private Integer giveMode;

    /**
     * 处方单特殊来源标识：1省中，邵逸夫医保小程序; 默认null
     */
    private Integer recipeSource;

    private Integer payFlag;//支付标志 0未支付，1已支付，2退款中，3退款成功，4支付失败

    private boolean isHiddenRecipeDetail;//是否隐方

    private List<RecipeDetailBean> recipeDetail;

    /**
     * 页面展示的按钮对象
     */
    private PayModeShowButtonBean buttons;

    /**
     * 页面展示的按钮集合
     */
    private GiveModeShowButtonVO giveModeShowButtonVO;

    /**
     * 签名通过处方笺文件
     */
    private String signFile;

    /**
     * 审核通过处方笺文件
     */
    private String chemistSignFile;

    /**
     * 跳转展示页面
     */
    private Integer jumpPageType;

    /**
     * 当前处方对应的订单code
     */
    private String orderCode;

    /**
     * 当前处方对应的开方医院
     */
    private Integer clinicOrgan;

    /**
     * 药企编码
     */
    private Integer enterpriseId;

    /**
     * 第三方跳转url
     */
    private String thirdUrl;

    @ItemProperty(alias = "物流对接类型 1-平台 2-药企")
    private Integer logisticsType;

    private String recipeCode;

    private String recipeNumber;

    public String getRecipeNumber() {
        return recipeNumber;
    }

    public void setRecipeNumber(String recipeNumber) {
        this.recipeNumber = recipeNumber;
    }

    public Integer getLogisticsType() {
        return logisticsType;
    }

    public void setLogisticsType(Integer logisticsType) {
        this.logisticsType = logisticsType;
    }

    public boolean getIsHiddenRecipeDetail() {
        return isHiddenRecipeDetail;
    }

    public void setIsHiddenRecipeDetail(boolean hiddenRecipe) {
        isHiddenRecipeDetail = hiddenRecipe;
    }

    public Integer getPayFlag() {
        return payFlag;
    }

    public void setPayFlag(Integer payFlag) {
        this.payFlag = payFlag;
    }

    public PatientTabStatusRecipeDTO() {
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Integer getJumpPageType() {
        return jumpPageType;
    }

    public void setJumpPageType(Integer jumpPageType) {
        this.jumpPageType = jumpPageType;
    }

    public String getSignFile() {
        return signFile;
    }

    public void setSignFile(String signFile) {
        this.signFile = signFile;
    }

    public String getChemistSignFile() {
        return chemistSignFile;
    }

    public void setChemistSignFile(String chemistSignFile) {
        this.chemistSignFile = chemistSignFile;
    }

    public PayModeShowButtonBean getButtons() {
        return buttons;
    }

    public void setButtons(PayModeShowButtonBean buttons) {
        this.buttons = buttons;
    }

    public GiveModeShowButtonVO getGiveModeShowButtonVO() {
        return giveModeShowButtonVO;
    }

    public void setGiveModeShowButtonVO(GiveModeShowButtonVO giveModeShowButtonVO) {
        this.giveModeShowButtonVO = giveModeShowButtonVO;
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

    public Integer getClinicOrgan() {
        return clinicOrgan;
    }

    public void setClinicOrgan(Integer clinicOrgan) {
        this.clinicOrgan = clinicOrgan;
    }

    public Integer getRecipeSource() {
        return recipeSource;
    }

    public void setRecipeSource(Integer recipeSource) {
        this.recipeSource = recipeSource;
    }

    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getThirdUrl() {
        return thirdUrl;
    }

    public void setThirdUrl(String thirdUrl) {
        this.thirdUrl = thirdUrl;
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
}
