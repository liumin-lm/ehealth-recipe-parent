package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * Created by  on 2016/6/14 0014.
 * 处方发送his 新增成功后 返回的数据组装类
 * @author zhongzx
 */
@Schema
public class OrderRepTO implements Serializable {

    private static final long serialVersionUID = -6695371157754652919L;

    /**
     * HIS系统病人唯一识别号
     */
    private String patientID;
    /**
     * 挂号序号
     */
    private String registerID;

    /**
     * 平台药品医嘱ID
     */
    private String orderID;

    /**
     * his处方号
     */
    private String recipeNo;

    /**
     * 处方金额
     */
    private String amount;

    /**
     * 医院系统医嘱号
     */
    private String orderNo;

    /**
     * 处方类型 1西药 2成药 3草药
     */
    private String recipeType;

    /**
     * 药品是否有库存
     */
    private String isDrugStock;

    /**
     * 组方号
     */
    private String setNo;

    /**
     * 处方中单个药品价格
     */
    private String price;

    /**
     * 药品规格
     */
    private String drugSpec;
    /**
     * 医保药品编码
     */
    private String medicalDrugCode;

    /**
     * 取药窗口
     */
    private String pharmNo;

    /**
     * 患者医保类型（编码）
     */
    private String medicalType;

    /**
     * 患者医保类型（名称）
     */
    private String medicalTypeText;

    /**
     * 备注
     */
    private String remark;

    private String drugCode;

    private String drugName;

    private String sendFlag;

    /**
     * 药品包装数量
     */
    private Integer pack;

    public String getIsDrugStock() {
        return isDrugStock;
    }

    public void setIsDrugStock(String isDrugStock) {
        this.isDrugStock = isDrugStock;
    }

    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getRecipeNo() {
        return recipeNo;
    }

    public void setRecipeNo(String recipeNo) {
        this.recipeNo = recipeNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getSetNo() {
        return setNo;
    }

    public void setSetNo(String setNo) {
        this.setNo = setNo;
    }

    public String getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(String recipeType) {
        this.recipeType = recipeType;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getPharmNo() {
        return pharmNo;
    }

    public void setPharmNo(String pharmNo) {
        this.pharmNo = pharmNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getSendFlag() {
        return sendFlag;
    }

    public void setSendFlag(String sendFlag) {
        this.sendFlag = sendFlag;
    }

    public String getRegisterID() {
        return registerID;
    }

    public void setRegisterID(String registerID) {
        this.registerID = registerID;
    }

    public String getMedicalType() {
        return medicalType;
    }

    public void setMedicalType(String medicalType) {
        this.medicalType = medicalType;
    }

    public String getMedicalTypeText() {
        return medicalTypeText;
    }

    public void setMedicalTypeText(String medicalTypeText) {
        this.medicalTypeText = medicalTypeText;
    }

    public String getDrugSpec() {
        return drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    public String getMedicalDrugCode() {
        return medicalDrugCode;
    }

    public void setMedicalDrugCode(String medicalDrugCode) {
        this.medicalDrugCode = medicalDrugCode;
    }

    public Integer getPack() {
        return pack;
    }

    public void setPack(Integer pack) {
        this.pack = pack;
    }
}
