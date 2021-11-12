package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author Created by liuxaiofeng on 2020/12/7.
 * 配送订单导出 处方订单实体
 */
@Entity
@Schema
@Access(AccessType.PROPERTY)
public class RecipeOrderExportDTO implements Serializable {
    private static final long serialVersionUID = -8356213511509296389L;

    @ItemProperty(alias = "订单ID")
    private Integer orderId;
    @ItemProperty(alias = "订单编号")
    private String orderCode;
    @ItemProperty(alias = "订单状态")
    @Dictionary(id = "eh.cdr.dictionary.RecipeOrderStatus")
    private Integer status;
    @ItemProperty(alias = "支付标志 0未支付，1已支付，2退款中，3退款成功，4支付失败")
    private Integer payFlag;
    @ItemProperty(alias = "配送费")
    private BigDecimal expressFee;
    @ItemProperty(alias = "订单总费用")
    private BigDecimal totalFee;
    @ItemProperty(alias = "实际支付费用")
    private Double actualPrice;
    @ItemProperty(alias = "交易流水号")
    private String tradeNo;
    @ItemProperty(alias = "药企ID")
    private Integer enterpriseId;
    @ItemProperty(alias = "收货人")
    private String receiver;
    @ItemProperty(alias = "收货人手机号")
    private String recMobile;
    @ItemProperty(alias = "地址（省）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address1;
    @ItemProperty(alias = "地址（市）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address2;
    @ItemProperty(alias = "地址（区县）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address3;
    @ItemProperty(alias = "地址（区县）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String streetAddress;
    @ItemProperty(alias = "详细地址")
    private String address4;
    @ItemProperty(alias = "创建时间")
    private Date createTime;
    @ItemProperty(alias = "支付时间")
    private Date payTime;
    @ItemProperty(alias = "购药方式")
    private Integer giveMode;
    @ItemProperty(alias = "期望配送日期")
    private String expectSendDate;
    @ItemProperty(alias = "期望配送时间")
    private String expectSendTime;
    @ItemProperty(alias = "配送主体类型 1医院配送 2药企配送")
    private Integer sendType;
    @ItemProperty(alias = "处方单id列表")
    private String recipeIdList;

    @ItemProperty(alias = "发药时间")
    private Date dispensingTime;

    @Column(name = "RecipeIdList")
    public String getRecipeIdList() {
        return recipeIdList;
    }

    public void setRecipeIdList(String recipeIdList) {
        this.recipeIdList = recipeIdList;
    }

    @Column(name = "send_type")
    public Integer getSendType() {
        return sendType;
    }

    public void setSendType(Integer sendType) {
        this.sendType = sendType;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "OrderId", unique = true, nullable = false)
    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    @Column(name = "OrderCode")
    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    @Column(name = "Status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "PayFlag")
    public Integer getPayFlag() {
        return payFlag;
    }

    public void setPayFlag(Integer payFlag) {
        this.payFlag = payFlag;
    }

    @Column(name = "ExpressFee")
    public BigDecimal getExpressFee() {
        return expressFee;
    }

    public void setExpressFee(BigDecimal expressFee) {
        this.expressFee = expressFee;
    }


    @Column(name = "TotalFee")
    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    @Column(name = "ActualPrice")
    public Double getActualPrice() {
        return actualPrice;
    }

    public void setActualPrice(Double actualPrice) {
        this.actualPrice = actualPrice;
    }

    @Column(name = "TradeNo")
    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    @Column(name = "EnterpriseId")
    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    @Column(name = "Receiver")
    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    @Column(name = "RecMobile")
    public String getRecMobile() {
        return recMobile;
    }

    public void setRecMobile(String recMobile) {
        this.recMobile = recMobile;
    }

    @Column(name = "Address1")
    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    @Column(name = "Address2")
    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    @Column(name = "Address3")
    public String getAddress3() {
        return address3;
    }

    public void setAddress3(String address3) {
        this.address3 = address3;
    }

    @Column(name = "streetAddress")
    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    @Column(name = "Address4")
    public String getAddress4() {
        return address4;
    }

    public void setAddress4(String address4) {
        this.address4 = address4;
    }

    @Column(name = "CreateTime")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "PayTime")
    public Date getPayTime() {
        return payTime;
    }

    public void setPayTime(Date payTime) {
        this.payTime = payTime;
    }

    @Column(name = "ExpectSendDate")
    public String getExpectSendDate() {
        return expectSendDate;
    }

    public void setExpectSendDate(String expectSendDate) {
        this.expectSendDate = expectSendDate;
    }

    @Column(name = "ExpectSendTime")
    public String getExpectSendTime() {
        return expectSendTime;
    }

    public void setExpectSendTime(String expectSendTime) {
        this.expectSendTime = expectSendTime;
    }

    @Transient
    public Integer getGiveMode() {
        return giveMode;
    }

    public void setGiveMode(Integer giveMode) {
        this.giveMode = giveMode;
    }

    public Date getDispensingTime() {
        return dispensingTime;
    }

    public void setDispensingTime(Date dispensingTime) {
        this.dispensingTime = dispensingTime;
    }
}
