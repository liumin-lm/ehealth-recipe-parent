package com.ngari.recipe.hisprescription.model;


import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;

import java.util.Date;

/**
 * 配送信息
 */
public class DeliveryInfo implements java.io.Serializable {

    private static final long serialVersionUID = 8564124312561977685L;
    //所选配送药企（店）代码
    private String deliveryCode;
    //所选配送药企（店）名称
    private String deliveryName;
    //收货人
    private String consignee;
    //收货人电话
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String consigneeTel;
    //收货地址代码
    private String receiveAddrCode;
    //收货地址名称
    private String receiveAddress;
    //收货人详细地址
    @Desensitizations(type = DesensitizationsType.ADDRESS)
    private String address;
    //期望收货日期
    private Date planDate;
    //期望配送时段
    private String planTime;

    public String getDeliveryCode() {
        return deliveryCode;
    }

    public void setDeliveryCode(String deliveryCode) {
        this.deliveryCode = deliveryCode;
    }

    public String getDeliveryName() {
        return deliveryName;
    }

    public void setDeliveryName(String deliveryName) {
        this.deliveryName = deliveryName;
    }

    public String getConsignee() {
        return consignee;
    }

    public void setConsignee(String consignee) {
        this.consignee = consignee;
    }

    public String getConsigneeTel() {
        return consigneeTel;
    }

    public void setConsigneeTel(String consigneeTel) {
        this.consigneeTel = consigneeTel;
    }

    public String getReceiveAddrCode() {
        return receiveAddrCode;
    }

    public void setReceiveAddrCode(String receiveAddrCode) {
        this.receiveAddrCode = receiveAddrCode;
    }

    public String getReceiveAddress() {
        return receiveAddress;
    }

    public void setReceiveAddress(String receiveAddress) {
        this.receiveAddress = receiveAddress;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Date getPlanDate() {
        return planDate;
    }

    public void setPlanDate(Date planDate) {
        this.planDate = planDate;
    }

    public String getPlanTime() {
        return planTime;
    }

    public void setPlanTime(String planTime) {
        this.planTime = planTime;
    }
}
