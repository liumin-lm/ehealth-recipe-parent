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
     * 是否可选择（平台）
     */
    private Boolean optional = true;
    /**
     * 按钮的展示形势(互联网+平台)
     */
    private Integer buttonType;
    /**
     * 互联网是否支持配送到家（互联网）
     */
    private Boolean givemode_send = false;
    /**
     * 互联网是否支持药店取药（互联网）
     */
    private Boolean givemode_tfds = false;
    /**
     * 互联网是否支持到院自取（互联网）
     */
    private Boolean givemode_hos = false;

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
        setGivemode_send(false);
        setGivemode_hos(false);
        setGivemode_tfds(false);
        setButtonType(0);
    }

    /**
     * 互联网模式的展示方式
     */
    public void internetModelButtons (){
        setOptional(true);
        setSupportDownload(false);
        setSupportOnline(false);
        setSupportTFDS(false);
        setSupportToHos(false);
        setButtonType(0);
    }

    /**
     * 平台模式的展示方式
     */
    public void platformModeButtons (){
        setOptional(false);
        setGivemode_hos(false);
        setGivemode_send(false);
        setGivemode_tfds(false);
        setButtonType(0);
    }

    public Boolean getGivemode_send() {
        return givemode_send;
    }

    public void setGivemode_send(Boolean givemode_send) {
        this.givemode_send = givemode_send;
    }

    public Boolean getGivemode_tfds() {
        return givemode_tfds;
    }

    public void setGivemode_tfds(Boolean givemode_tfds) {
        this.givemode_tfds = givemode_tfds;
    }

    public Boolean getGivemode_hos() {
        return givemode_hos;
    }

    public void setGivemode_hos(Boolean givemode_hos) {
        this.givemode_hos = givemode_hos;
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