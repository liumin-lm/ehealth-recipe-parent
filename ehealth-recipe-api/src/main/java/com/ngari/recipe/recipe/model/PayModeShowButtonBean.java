package com.ngari.recipe.recipe.model;


import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
* @Description: 展示购药方式按钮
* @Author: JRK
* @Date: 2019/8/19
*/
@Schema
public class PayModeShowButtonBean implements Serializable{
    private static final long serialVersionUID = 1756279114762256344L;
    /**
     * 药店取药购药方式（平台）
     */
    private Boolean supportTFDS = false;
    /**
     * 配送到家购药方式（平台）
     */
    private Boolean supportOnline = false;
    /**
     * 下载处方购药方式（平台）
     */
    private Boolean supportDownload = false;
    /**
     * 到院取药购药方式（平台）
     */
    private Boolean supportToHos = false;

    /**
     * 用药指导显示按钮
     */
    private Boolean supportMedicationGuide = false;
    /**
     * 是否可选择（平台）
     */
    private Boolean optional = true;
    /**
     * 按钮的展示形势(互联网+平台)
     */
    private Integer buttonType;

    /**
     * 配送到家药企配送
     */
    private Boolean showSendToEnterprises = false;

    /**
     * 配送到家医院配送
     */
    private Boolean showSendToHos = false;

    public PayModeShowButtonBean() {
    }

    public PayModeShowButtonBean(Boolean supportTFDS, Boolean supportOnline, Boolean supportToHos, Boolean supportDownload, Boolean optional) {
        this.supportTFDS = supportTFDS;
        this.supportOnline = supportOnline;
        this.supportToHos = supportToHos;
        this.supportDownload = supportDownload;
        this.optional = optional;
    }

    /**
     * 无用的按钮展示方式
     */
    public void noUserButtons (){
        setOptional(false);
        setSupportDownload(false);
        setSupportOnline(false);
        setSupportTFDS(false);
        setSupportToHos(false);
        setButtonType(0);
        setShowSendToEnterprises(false);
        setShowSendToHos(false);
    }

    public Boolean getShowSendToEnterprises() {
        return showSendToEnterprises;
    }

    public void setShowSendToEnterprises(Boolean showSendToEnterprises) {
        this.showSendToEnterprises = showSendToEnterprises;
    }

    public Boolean getShowSendToHos() {
        return showSendToHos;
    }

    public void setShowSendToHos(Boolean showSendToHos) {
        this.showSendToHos = showSendToHos;
    }

    public Integer getButtonType() {
        return buttonType;
    }

    public void setButtonType(Integer buttonType) {
        this.buttonType = buttonType;
    }

    public Boolean getSupportToHos() {
        return supportToHos;
    }

    public void setSupportToHos(Boolean supportToHos) {
        this.supportToHos = supportToHos;
    }

    public Boolean getSupportOnline() {
        return supportOnline;
    }

    public void setSupportOnline(Boolean supportOnline) {
        this.supportOnline = supportOnline;
    }

    public Boolean getSupportDownload() {
        return supportDownload;
    }

    public void setSupportDownload(Boolean supportDownload) {
        this.supportDownload = supportDownload;
    }

    public Boolean getSupportTFDS() {
        return supportTFDS;
    }

    public void setSupportTFDS(Boolean supportTFDS) {
        this.supportTFDS = supportTFDS;
    }

    public Boolean getOptional() {
        return optional;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

    public Boolean getSupportMedicationGuide() {
        return supportMedicationGuide;
    }

    public void setSupportMedicationGuide(Boolean supportMedicationGuide) {
        this.supportMedicationGuide = supportMedicationGuide;
    }

    @Override
    public String toString() {
        return "PayModeShowButtonBean{" +
                "supportTFDS=" + supportTFDS +
                ", supportOnline=" + supportOnline +
                ", supportDownload=" + supportDownload +
                ", supportToHos=" + supportToHos +
                ", optional=" + optional +
                '}';
    }
}