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
     * 取药窗口
     */
    private String pharmNo;

    /**
     * 备注
     */
    private String remark;

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
}
