package com.ngari.recipe.regulation.model;

import java.io.Serializable;

public class RegulationRecipeInfoDTO implements Serializable{
    private static final long serialVersionUID = 7654003831276249875L;

    private Integer organId; //机构id

    private String recipeId; //处方号

    private String returnVisitId; //复诊流水号

    private String cancelFlag; //撤销标志

    private String cardId; //卡号

    private String cardTpe; //卡类型

    private String certificate; //证件号

    private String certificateType; //证件类型

    private String visitDepartId; //就诊科室代码

    private String prescribeDoctorId; //开方医生代码

    private String prescribeDoctorName; //开方医生姓名

    private String prescribeDoctorCertId; //开方医生身份证号

    private String recipeType; //处方类型

    private String payFlag; //收费标志 1是  0否

    private String signValue; //处方开具签名值

    private String serialNumCA; //医护人员证件序列号 上海CA签发证书序列号

    private String signPharmacistCADate; //可信时间戳

    private String medicDoctorName; //药师姓名

    private String medicDoctorCertId; //药师身份证号

    private String modifyFlag; //修改标志

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getReturnVisitId() {
        return returnVisitId;
    }

    public void setReturnVisitId(String returnVisitId) {
        this.returnVisitId = returnVisitId;
    }

    public String getCancelFlag() {
        return cancelFlag;
    }

    public void setCancelFlag(String cancelFlag) {
        this.cancelFlag = cancelFlag;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getCardTpe() {
        return cardTpe;
    }

    public void setCardTpe(String cardTpe) {
        this.cardTpe = cardTpe;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }

    public String getVisitDepartId() {
        return visitDepartId;
    }

    public void setVisitDepartId(String visitDepartId) {
        this.visitDepartId = visitDepartId;
    }

    public String getPrescribeDoctorId() {
        return prescribeDoctorId;
    }

    public void setPrescribeDoctorId(String prescribeDoctorId) {
        this.prescribeDoctorId = prescribeDoctorId;
    }

    public String getPrescribeDoctorName() {
        return prescribeDoctorName;
    }

    public void setPrescribeDoctorName(String prescribeDoctorName) {
        this.prescribeDoctorName = prescribeDoctorName;
    }

    public String getPrescribeDoctorCertId() {
        return prescribeDoctorCertId;
    }

    public void setPrescribeDoctorCertId(String prescribeDoctorCertId) {
        this.prescribeDoctorCertId = prescribeDoctorCertId;
    }

    public String getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(String recipeType) {
        this.recipeType = recipeType;
    }

    public String getPayFlag() {
        return payFlag;
    }

    public void setPayFlag(String payFlag) {
        this.payFlag = payFlag;
    }

    public String getSignValue() {
        return signValue;
    }

    public void setSignValue(String signValue) {
        this.signValue = signValue;
    }

    public String getSerialNumCA() {
        return serialNumCA;
    }

    public void setSerialNumCA(String serialNumCA) {
        this.serialNumCA = serialNumCA;
    }

    public String getSignPharmacistCADate() {
        return signPharmacistCADate;
    }

    public void setSignPharmacistCADate(String signPharmacistCADate) {
        this.signPharmacistCADate = signPharmacistCADate;
    }

    public String getMedicDoctorName() {
        return medicDoctorName;
    }

    public void setMedicDoctorName(String medicDoctorName) {
        this.medicDoctorName = medicDoctorName;
    }

    public String getMedicDoctorCertId() {
        return medicDoctorCertId;
    }

    public void setMedicDoctorCertId(String medicDoctorCertId) {
        this.medicDoctorCertId = medicDoctorCertId;
    }

    public String getModifyFlag() {
        return modifyFlag;
    }

    public void setModifyFlag(String modifyFlag) {
        this.modifyFlag = modifyFlag;
    }
}
