package com.ngari.recipe.common;

import ctd.schema.annotation.ItemProperty;

import java.io.Serializable;

/**
 * @author Created by liuxiaofeng on 2020/10/26.
 */
public class RecipeOrderBillReqTO implements Serializable{
    private static final long serialVersionUID = 7018685892567902012L;

    @ItemProperty(alias = "处方订单号")
    private String recipeOrderCode;

    @ItemProperty(alias = "电子票据代码")
    private String billBathCode;

    @ItemProperty(alias = "电子票据号码")
    private String billNumber;

    @ItemProperty(alias = "电子票据二维码")
    private String billQrCode;

    @ItemProperty(alias = "电子票据h5地址")
    private String billPictureUrl;

    public String getRecipeOrderCode() {
        return recipeOrderCode;
    }

    public void setRecipeOrderCode(String recipeOrderCode) {
        this.recipeOrderCode = recipeOrderCode;
    }

    public String getBillBathCode() {
        return billBathCode;
    }

    public void setBillBathCode(String billBathCode) {
        this.billBathCode = billBathCode;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public String getBillQrCode() {
        return billQrCode;
    }

    public void setBillQrCode(String billQrCode) {
        this.billQrCode = billQrCode;
    }

    public String getBillPictureUrl() {
        return billPictureUrl;
    }

    public void setBillPictureUrl(String billPictureUrl) {
        this.billPictureUrl = billPictureUrl;
    }
}
