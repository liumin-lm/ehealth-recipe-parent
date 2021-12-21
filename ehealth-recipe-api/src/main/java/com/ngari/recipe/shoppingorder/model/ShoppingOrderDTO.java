package com.ngari.recipe.shoppingorder.model;

import ctd.schema.annotation.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author liuyuang
 */
@Schema
public class ShoppingOrderDTO implements java.io.Serializable {

    private static final long serialVersionUID = -2743073217673860563L;

    private Integer orderId;
    private String mpiId;
    @ItemProperty(alias = "患者姓名")
    private String patientName;
    @ItemProperty(alias = "订单编号")
    private String orderCode;
    @ItemProperty(alias = "订单状态")
    @Dictionary(id = "eh.cdr.dictionary.ShoppingStatus")
    private Integer status;
    @ItemProperty(alias = "下单时间")
    private Date saleTime;
    @ItemProperty(alias = "订单原总价")
    private BigDecimal totalFee;
    @ItemProperty(alias = "订单实际支付金额")
    private BigDecimal actualFee;
    @ItemProperty(alias = "药品部分总价")
    private BigDecimal drugFee;
    @ItemProperty(alias = "订单配送费")
    private BigDecimal expressFee;
    @ItemProperty(alias = "订单优惠金额")
    private BigDecimal couponFee;
    @ItemProperty(alias = "支付方式")
    @Dictionary(id = "eh.cdr.dictionary.ShoppingPayWay")
    private Integer payWay;
    @ItemProperty(alias = "收货人")
    private String receiver;
    @ItemProperty(alias = "收货联系号码")
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String recMobile;
    @ItemProperty(alias = "收货地址")
    @Desensitizations(type = DesensitizationsType.ADDRESS)
    private String address;
    @ItemProperty(alias = "订单取消时间")
    private Date cancelTime;
    @ItemProperty(alias = "订单取消原因")
    private String cancelReason;
    @ItemProperty(alias = "物流公司")
    private String logisticsCompany;
    @ItemProperty(alias = "快递单号")
    private String trackingNumber;
    @ItemProperty(alias = "创建时间")
    private Date createTime;
    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    public ShoppingOrderDTO() {
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getMpiId() {
        return mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getSaleTime() {
        return saleTime;
    }

    public void setSaleTime(Date saleTime) {
        this.saleTime = saleTime;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    public BigDecimal getActualFee() {
        return actualFee;
    }

    public void setActualFee(BigDecimal actualFee) {
        this.actualFee = actualFee;
    }

    public BigDecimal getDrugFee() {
        return drugFee;
    }

    public void setDrugFee(BigDecimal drugFee) {
        this.drugFee = drugFee;
    }

    public BigDecimal getExpressFee() {
        return expressFee;
    }

    public void setExpressFee(BigDecimal expressFee) {
        this.expressFee = expressFee;
    }

    public BigDecimal getCouponFee() {
        return couponFee;
    }

    public void setCouponFee(BigDecimal couponFee) {
        this.couponFee = couponFee;
    }

    public Integer getPayWay() {
        return payWay;
    }

    public void setPayWay(Integer payWay) {
        this.payWay = payWay;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getRecMobile() {
        return recMobile;
    }

    public void setRecMobile(String recMobile) {
        this.recMobile = recMobile;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Date getCancelTime() {
        return cancelTime;
    }

    public void setCancelTime(Date cancelTime) {
        this.cancelTime = cancelTime;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getLogisticsCompany() {
        return logisticsCompany;
    }

    public void setLogisticsCompany(String logisticsCompany) {
        this.logisticsCompany = logisticsCompany;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }
}
