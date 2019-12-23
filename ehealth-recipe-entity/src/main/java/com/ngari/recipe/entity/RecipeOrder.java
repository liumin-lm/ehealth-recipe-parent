package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.FileToken;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/2/13.
 */
@Entity
@Schema
@Table(name = "cdr_recipeorder")
@Access(AccessType.PROPERTY)
public class RecipeOrder implements Serializable {
    private static final long serialVersionUID = -1L;

    @ItemProperty(alias = "订单ID")
    private Integer orderId;

    @ItemProperty(alias = "订单编号")
    private String orderCode;

    @ItemProperty(alias = "患者编号")
    private String mpiId;

    @ItemProperty(alias = "开方机构")
    private Integer organId;

    @ItemProperty(alias = "订单状态")
    @Dictionary(id = "eh.cdr.dictionary.RecipeOrderStatus")
    private Integer status;

    @ItemProperty(alias = "订单是否有效 1有效，0表示该订单已取消或者无效临时订单")
    private Integer effective;

    @ItemProperty(alias = "0没有子订单，1存在子订单")
    private Integer divisionFlag;

    @ItemProperty(alias = "处方单id列表")
    private String recipeIdList;

    @ItemProperty(alias = "支付标志 0未支付，1已支付，2退款中，3退款成功，4支付失败")
    private Integer payFlag;

    @ItemProperty(alias = "优惠券ID")
    private Integer couponId;

    @ItemProperty(alias = "优惠券name")
    private String couponName;

    @ItemProperty(alias = "优惠金额")
    private BigDecimal couponFee;

    @ItemProperty(alias = "优惠描述")
    private String couponDesc;

    @ItemProperty(alias = "挂号费")
    private BigDecimal registerFee;

    @ItemProperty(alias = "配送费")
    private BigDecimal expressFee;

    @ItemProperty(alias = "处方总费用")
    private BigDecimal recipeFee;

    @ItemProperty(alias = "订单总费用")
    private BigDecimal totalFee;

    @ItemProperty(alias = "实际支付费用")
    private Double actualPrice;

    @ItemProperty(alias = "交易流水号")
    private String tradeNo;

    @ItemProperty(alias = "支付方式")
    private String wxPayWay;

    @ItemProperty(alias = "商户订单号")
    private String outTradeNo;

    @ItemProperty(alias = "支付平台分配的机构id")
    private String payOrganId;

    @ItemProperty(alias = "微信支付错误码")
    private String wxPayErrorCode;

    @ItemProperty(alias = "药企ID")
    private Integer enterpriseId;

    @ItemProperty(alias = "药企订单编号")
    private String depSn;

    @ItemProperty(alias = "药企推送标志位, 0未推送，1已推送, -1推送失败")
    private Integer pushFlag;

    @ItemProperty(alias = "地址编号")
    private Integer addressID;

    @ItemProperty(alias = "收货人")
    private String receiver;

    @ItemProperty(alias = "收货人手机号")
    private String recMobile;

    @ItemProperty(alias = "收货人电话")
    private String recTel;

    @ItemProperty(alias = "地址（省）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address1;

    @ItemProperty(alias = "地址（市）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address2;

    @ItemProperty(alias = "地址（区县）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address3;

    @ItemProperty(alias = "详细地址")
    private String address4;

    @ItemProperty(alias = "邮政编码")
    private String zipCode;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "支付时间")
    private Date payTime;

    @ItemProperty(alias = "发货时间")
    private Date sendTime;

    @ItemProperty(alias = "完成时间")
    private Date finishTime;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModifyTime;

    private List<? extends Object> list;

    @ItemProperty(alias = "当前地址是否可进行配送")
    private Boolean addressCanSend;

    @ItemProperty(alias = "物流公司")
    @Dictionary(id = "eh.cdr.dictionary.LogisticsCompany")
    private Integer logisticsCompany;

    @ItemProperty(alias = "快递单号")
    private String trackingNumber;

    @ItemProperty(alias = "药店名称")
    private String drugStoreName;

    @ItemProperty(alias = "药店地址")
    private String drugStoreAddr;

    @ItemProperty(alias = "代煎费")
    private BigDecimal decoctionFee;

    @ItemProperty(alias = "审核费")
    private BigDecimal auditFee;

    @ItemProperty(alias = "其余费用")
    private BigDecimal otherFee;

    @ItemProperty(alias = "机构代煎单价")
    private BigDecimal decoctionUnitPrice;

    @ItemProperty(alias = "剂数")
    private Integer copyNum;

    @ItemProperty(alias = "完整地址")
    private String completeAddress;

    @ItemProperty(alias = "取消原因")
    private String cancelReason;

    @ItemProperty(alias = "药店编码")
    private String drugStoreCode;

    @ItemProperty(alias = "处方流转模式")
    private String recipeMode;

    @ItemProperty(alias = "购药方式")
    private Integer giveMode;

    @ItemProperty(alias = "药企名称")
    private String enterpriseName;

    @ItemProperty(alias = "期望配送日期")
    private String expectSendDate;

    @ItemProperty(alias = "期望配送时间")
    private String expectSendTime;

    @ItemProperty(alias = "运费细则图片ID")
    @FileToken(expires = 3600)
    private String transFeeDetail;

    @ItemProperty(alias = "医保结算订单号")
    private String settleOrderNo;

    @ItemProperty(alias = "医保结算人脸识别token")
    private String smkFaceToken;

    @Column(name = "cancelReason")
    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public RecipeOrder() {
        initData();
    }

    public void initData() {
        this.setEffective(1);
        this.setDivisionFlag(0);
        //未支付
        this.setPayFlag(0);
        BigDecimal zero = BigDecimal.ZERO;
        this.setCouponFee(zero);
        this.setExpressFee(zero);
        this.setDecoctionFee(zero);
        this.setTotalFee(zero);
        this.setActualPrice(0d);
        this.setPushFlag(0);
        Date now = Calendar.getInstance().getTime();
        this.setCreateTime(now);
        this.setLastModifyTime(now);
        this.setAuditFee(zero);
        this.setOtherFee(zero);
    }

    public String getCouponDesc() {
        return couponDesc;
    }

    public void setCouponDesc(String couponDesc) {
        this.couponDesc = couponDesc;
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

    @Column(name = "MpiId")
    public String getMpiId() {
        return mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
    }

    @Column(name = "OrganId")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "Status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "Effective")
    public Integer getEffective() {
        return effective;
    }

    public void setEffective(Integer effective) {
        this.effective = effective;
    }

    @Column(name = "DivisionFlag")
    public Integer getDivisionFlag() {
        return divisionFlag;
    }

    public void setDivisionFlag(Integer divisionFlag) {
        this.divisionFlag = divisionFlag;
    }

    @Column(name = "RecipeIdList")
    public String getRecipeIdList() {
        return recipeIdList;
    }

    public void setRecipeIdList(String recipeIdList) {
        this.recipeIdList = recipeIdList;
    }

    @Column(name = "PayFlag")
    public Integer getPayFlag() {
        return payFlag;
    }

    public void setPayFlag(Integer payFlag) {
        this.payFlag = payFlag;
    }

    @Column(name = "CouponId")
    public Integer getCouponId() {
        return couponId;
    }

    public void setCouponId(Integer couponId) {
        this.couponId = couponId;
    }

    @Column(name = "CouponFee")
    public BigDecimal getCouponFee() {
        return couponFee;
    }

    public void setCouponFee(BigDecimal couponFee) {
        this.couponFee = couponFee;
    }

    @Column(name = "RegisterFee")
    public BigDecimal getRegisterFee() {
        return registerFee;
    }

    public void setRegisterFee(BigDecimal registerFee) {
        this.registerFee = registerFee;
    }

    @Column(name = "ExpressFee")
    public BigDecimal getExpressFee() {
        return expressFee;
    }

    public void setExpressFee(BigDecimal expressFee) {
        this.expressFee = expressFee;
    }

    @Column(name = "AuditFee")
    public BigDecimal getAuditFee() {
        return auditFee;
    }

    public void setAuditFee(BigDecimal auditFee) {
        this.auditFee = auditFee;
    }

    @Column(name = "OtherFee")
    public BigDecimal getOtherFee() {
        return otherFee;
    }

    public void setOtherFee(BigDecimal otherFee) {
        this.otherFee = otherFee;
    }

    @Column(name = "RecipeFee")
    public BigDecimal getRecipeFee() {
        return recipeFee;
    }

    public void setRecipeFee(BigDecimal recipeFee) {
        this.recipeFee = recipeFee;
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

    @Column(name = "CouponName")
    public String getCouponName() {
        return couponName;
    }

    public void setCouponName(String couponName) {
        this.couponName = couponName;
    }

    @Column(name = "TradeNo")
    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    @Column(name = "WxPayWay")
    public String getWxPayWay() {
        return wxPayWay;
    }

    public void setWxPayWay(String wxPayWay) {
        this.wxPayWay = wxPayWay;
    }

    @Column(name = "OutTradeNo")
    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    @Column(name = "PayOrganId")
    public String getPayOrganId() {
        return payOrganId;
    }

    public void setPayOrganId(String payOrganId) {
        this.payOrganId = payOrganId;
    }

    @Column(name = "WxPayErrorCode")
    public String getWxPayErrorCode() {
        return wxPayErrorCode;
    }

    public void setWxPayErrorCode(String wxPayErrorCode) {
        this.wxPayErrorCode = wxPayErrorCode;
    }

    @Column(name = "EnterpriseId")
    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    @Column(name = "DepSn")
    public String getDepSn() {
        return depSn;
    }

    public void setDepSn(String depSn) {
        this.depSn = depSn;
    }

    @Column(name = "PushFlag")
    public Integer getPushFlag() {
        return pushFlag;
    }

    public void setPushFlag(Integer pushFlag) {
        this.pushFlag = pushFlag;
    }

    @Column(name = "AddressID")
    public Integer getAddressID() {
        return addressID;
    }

    public void setAddressID(Integer addressID) {
        this.addressID = addressID;
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

    @Column(name = "RecTel")
    public String getRecTel() {
        return recTel;
    }

    public void setRecTel(String recTel) {
        this.recTel = recTel;
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

    @Column(name = "Address4")
    public String getAddress4() {
        return address4;
    }

    public void setAddress4(String address4) {
        this.address4 = address4;
    }

    @Column(name = "ZipCode")
    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
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

    @Column(name = "SendTime")
    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    @Column(name = "FinishTime")
    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    @Column(name = "LastModifyTime")
    public Date getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(Date lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    @Transient
    public List<? extends Object> getList() {
        return list;
    }

    public void setList(List<? extends Object> list) {
        this.list = list;
    }

    @Transient
    public Boolean getAddressCanSend() {
        return addressCanSend;
    }

    public void setAddressCanSend(Boolean addressCanSend) {
        this.addressCanSend = addressCanSend;
    }

    @Column(name = "LogisticsCompany")
    public Integer getLogisticsCompany() {
        return logisticsCompany;
    }

    public void setLogisticsCompany(Integer logisticsCompany) {
        this.logisticsCompany = logisticsCompany;
    }

    @Column(name = "TrackingNumber")
    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    @Column(name = "DrugStoreName")
    public String getDrugStoreName() {
        return drugStoreName;
    }

    public void setDrugStoreName(String drugStoreName) {
        this.drugStoreName = drugStoreName;
    }

    @Column(name = "DrugStoreAddr")
    public String getDrugStoreAddr() {
        return drugStoreAddr;
    }

    public void setDrugStoreAddr(String drugStoreAddr) {
        this.drugStoreAddr = drugStoreAddr;
    }

    @Column(name = "DecoctionFee")
    public BigDecimal getDecoctionFee() {
        return decoctionFee;
    }

    public void setDecoctionFee(BigDecimal decoctionFee) {
        this.decoctionFee = decoctionFee;
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
    public BigDecimal getDecoctionUnitPrice() {
        return decoctionUnitPrice;
    }

    public void setDecoctionUnitPrice(BigDecimal decoctionUnitPrice) {
        this.decoctionUnitPrice = decoctionUnitPrice;
    }

    @Transient
    public Integer getCopyNum() {
        return copyNum;
    }

    public void setCopyNum(Integer copyNum) {
        this.copyNum = copyNum;
    }

    @Transient
    public String getCompleteAddress() {
        return completeAddress;
    }

    public void setCompleteAddress(String completeAddress) {
        this.completeAddress = completeAddress;
    }

    @Column(name = "drugStoreCode")
    public String getDrugStoreCode() {
        return drugStoreCode;
    }

    public void setDrugStoreCode(String drugStoreCode) {
        this.drugStoreCode = drugStoreCode;
    }

    @Transient
    public String getRecipeMode() {
        return recipeMode;
    }

    public void setRecipeMode(String recipeMode) {
        this.recipeMode = recipeMode;
    }

    @Transient
    public Integer getGiveMode() {
        return giveMode;
    }

    public void setGiveMode(Integer giveMode) {
        this.giveMode = giveMode;
    }

    @Transient
    public String getEnterpriseName() {
        return enterpriseName;
    }

    public void setEnterpriseName(String enterpriseName) {
        this.enterpriseName = enterpriseName;
    }

    @Column(name = "settleOrderNo")
    public String getSettleOrderNo() {
        return settleOrderNo;
    }

    public void setSettleOrderNo(String settleOrderNo) {
        this.settleOrderNo = settleOrderNo;
    }

    @Column(name = "smkFaceToken")
    public String getSmkFaceToken() {
        return smkFaceToken;
    }

    public void setSmkFaceToken(String smkFaceToken) {
        this.smkFaceToken = smkFaceToken;
    }

    @Column(name = "transFeeDetail")
    public String getTransFeeDetail() {
        return transFeeDetail;
    }

    public void setTransFeeDetail(String transFeeDetail) {
        this.transFeeDetail = transFeeDetail;
    }
}
