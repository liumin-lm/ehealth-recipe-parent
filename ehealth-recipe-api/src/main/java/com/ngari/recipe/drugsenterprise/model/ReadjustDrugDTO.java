package com.ngari.recipe.drugsenterprise.model;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\12\10 0010 14:18
 */
public class ReadjustDrugDTO implements Serializable{
    private static final long serialVersionUID = -5374616430917490731L;

    private String account;

    private Integer hospitalId;

    private String drugCode;

    private Double price;

    private String appKey;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Integer getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Integer hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }
}
