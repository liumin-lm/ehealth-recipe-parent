package com.ngari.recipe.recipeorder.model;

import ctd.schema.annotation.ItemProperty;

/**
 * 药师信息用于前端展示
 *
 * @author fuzi
 */
public class ApothecaryVO {

    @ItemProperty(alias = "订单ID")
    private Integer orderId;

    @ItemProperty(alias = "审核药师姓名")
    private String checkApothecaryName;

    @ItemProperty(alias = "审核药师身份证")
    private String checkApothecaryIdCard;

    @ItemProperty(alias = "发药药师姓名")
    private String dispensingApothecaryName;

    @ItemProperty(alias = "发药药师身份证")
    private String dispensingApothecaryIdCard;

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getCheckApothecaryName() {
        return checkApothecaryName;
    }

    public void setCheckApothecaryName(String checkApothecaryName) {
        this.checkApothecaryName = checkApothecaryName;
    }

    public String getCheckApothecaryIdCard() {
        return checkApothecaryIdCard;
    }

    public void setCheckApothecaryIdCard(String checkApothecaryIdCard) {
        this.checkApothecaryIdCard = checkApothecaryIdCard;
    }

    public String getDispensingApothecaryName() {
        return dispensingApothecaryName;
    }

    public void setDispensingApothecaryName(String dispensingApothecaryName) {
        this.dispensingApothecaryName = dispensingApothecaryName;
    }

    public String getDispensingApothecaryIdCard() {
        return dispensingApothecaryIdCard;
    }

    public void setDispensingApothecaryIdCard(String dispensingApothecaryIdCard) {
        this.dispensingApothecaryIdCard = dispensingApothecaryIdCard;
    }
}
