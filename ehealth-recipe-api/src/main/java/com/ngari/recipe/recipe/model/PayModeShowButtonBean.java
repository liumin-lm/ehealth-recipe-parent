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
     * 药店取药购药方式
     */
    private Boolean supportTFDS = false;
    /**
     * 配送到家购药方式
     */
    private Boolean supportOnline = false;
    /**
     * 下载处方购药方式
     */
    private Boolean supportDownload = false;
    /**
     * 到院取药购药方式
     */
    private Boolean supportToHos = false;
    /**
     * 是否可选择
     */
    private Boolean optional = true;

    public PayModeShowButtonBean() {
    }

    public PayModeShowButtonBean(Boolean supportTFDS, Boolean supportOnline, Boolean supportToHos, Boolean supportDownload, Boolean optional) {
        this.supportTFDS = supportTFDS;
        this.supportOnline = supportOnline;
        this.supportToHos = supportToHos;
        this.supportDownload = supportDownload;
        this.optional = optional;
    }
    public void noUserButtons (){
        setOptional(false);
        setSupportDownload(false);
        setSupportOnline(false);
        setSupportTFDS(false);
        setSupportToHos(false);
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