package com.ngari.recipe.recipeorder.model;

import ctd.schema.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author: JRK
 * @date:209/10/18. 处方以及订单信息
 */
@Schema
public class RecipeOrderInfoBean implements Serializable {

    private static final long serialVersionUID = -1365227235362189226L;

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

    @ItemProperty(alias = "挂号费")
    private BigDecimal registerFee;

    @ItemProperty(alias = "配送费")
    private BigDecimal expressFee;

    @ItemProperty(alias = "审核费")
    private BigDecimal auditFee;

    @ItemProperty(alias = "其余费用")
    private BigDecimal otherFee;

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

    @Desensitizations(type = DesensitizationsType.MOBILE)
    @ItemProperty(alias = "收货人手机号")
    private String recMobile;

    @Desensitizations(type = DesensitizationsType.MOBILE)
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
    @Desensitizations(type = DesensitizationsType.ADDRESS)
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

    @ItemProperty(alias = "机构代煎单价")
    private BigDecimal decoctionUnitPrice;

    @ItemProperty(alias = "中医辨证论治费")
    private BigDecimal tcmFee;

    @ItemProperty(alias = "剂数")
    private Integer copyNum;

    @ItemProperty(alias = "完整地址")
    @Desensitizations(type = DesensitizationsType.ADDRESS)
    private String completeAddress;

    @ItemProperty(alias = "处方流转模式")
    private String recipeMode;

    @ItemProperty(alias = "购药方式")
    private Integer giveMode;

    @ItemProperty(alias = "药企名称")
    private String enterpriseName;

    //date 2019/10/18
    //添加优惠卷信息
    @ItemProperty(alias = "优惠券描述")
    private String couponDesc;

    //date 2019/10/18
    //添加优惠卷信息
    @ItemProperty(alias = "处方详情描述")
    private Map<String, Object> recipeInfoMap;

    public RecipeOrderInfoBean() {
        initData();
    }

    public void initData() {
        this.setEffective(1);
        this.setDivisionFlag(0);
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
        this.setAuditFee(zero);
        this.setOtherFee(zero);
    }

    public Map<String, Object> getRecipeInfoMap() {
        return recipeInfoMap;
    }

    public void setRecipeInfoMap(Map<String, Object> recipeInfoMap) {
        this.recipeInfoMap = recipeInfoMap;
    }

    public String getCouponDesc() {
        return couponDesc;
    }

    public void setCouponDesc(String couponDesc) {
        this.couponDesc = couponDesc;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getMpiId() {
        return mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getEffective() {
        return effective;
    }

    public void setEffective(Integer effective) {
        this.effective = effective;
    }

    public Integer getDivisionFlag() {
        return divisionFlag;
    }

    public void setDivisionFlag(Integer divisionFlag) {
        this.divisionFlag = divisionFlag;
    }

    public String getRecipeIdList() {
        return recipeIdList;
    }

    public void setRecipeIdList(String recipeIdList) {
        this.recipeIdList = recipeIdList;
    }

    public Integer getPayFlag() {
        return payFlag;
    }

    public void setPayFlag(Integer payFlag) {
        this.payFlag = payFlag;
    }

    public Integer getCouponId() {
        return couponId;
    }

    public void setCouponId(Integer couponId) {
        this.couponId = couponId;
    }

    public String getCouponName() {
        return couponName;
    }

    public void setCouponName(String couponName) {
        this.couponName = couponName;
    }

    public BigDecimal getCouponFee() {
        return couponFee;
    }

    public void setCouponFee(BigDecimal couponFee) {
        this.couponFee = couponFee;
    }

    public BigDecimal getRegisterFee() {
        return registerFee;
    }

    public void setRegisterFee(BigDecimal registerFee) {
        this.registerFee = registerFee;
    }

    public BigDecimal getExpressFee() {
        return expressFee;
    }

    public void setExpressFee(BigDecimal expressFee) {
        this.expressFee = expressFee;
    }

    public BigDecimal getAuditFee() {
        return auditFee;
    }

    public void setAuditFee(BigDecimal auditFee) {
        this.auditFee = auditFee;
    }

    public BigDecimal getOtherFee() {
        return otherFee;
    }

    public void setOtherFee(BigDecimal otherFee) {
        this.otherFee = otherFee;
    }

    public BigDecimal getRecipeFee() {
        return recipeFee;
    }

    public void setRecipeFee(BigDecimal recipeFee) {
        this.recipeFee = recipeFee;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    public Double getActualPrice() {
        return actualPrice;
    }

    public void setActualPrice(Double actualPrice) {
        this.actualPrice = actualPrice;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    public String getWxPayWay() {
        return wxPayWay;
    }

    public void setWxPayWay(String wxPayWay) {
        this.wxPayWay = wxPayWay;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public String getPayOrganId() {
        return payOrganId;
    }

    public void setPayOrganId(String payOrganId) {
        this.payOrganId = payOrganId;
    }

    public String getWxPayErrorCode() {
        return wxPayErrorCode;
    }

    public void setWxPayErrorCode(String wxPayErrorCode) {
        this.wxPayErrorCode = wxPayErrorCode;
    }

    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getDepSn() {
        return depSn;
    }

    public void setDepSn(String depSn) {
        this.depSn = depSn;
    }

    public Integer getPushFlag() {
        return pushFlag;
    }

    public void setPushFlag(Integer pushFlag) {
        this.pushFlag = pushFlag;
    }

    public Integer getAddressID() {
        return addressID;
    }

    public void setAddressID(Integer addressID) {
        this.addressID = addressID;
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

    public String getRecTel() {
        return recTel;
    }

    public void setRecTel(String recTel) {
        this.recTel = recTel;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getAddress3() {
        return address3;
    }

    public void setAddress3(String address3) {
        this.address3 = address3;
    }

    public String getAddress4() {
        return address4;
    }

    public void setAddress4(String address4) {
        this.address4 = address4;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getPayTime() {
        return payTime;
    }

    public void setPayTime(Date payTime) {
        this.payTime = payTime;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public Date getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(Date lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    public List<? extends Object> getList() {
        return list;
    }

    public void setList(List<? extends Object> list) {
        this.list = list;
    }

    public Boolean getAddressCanSend() {
        return addressCanSend;
    }

    public void setAddressCanSend(Boolean addressCanSend) {
        this.addressCanSend = addressCanSend;
    }

    public Integer getLogisticsCompany() {
        return logisticsCompany;
    }

    public void setLogisticsCompany(Integer logisticsCompany) {
        this.logisticsCompany = logisticsCompany;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getDrugStoreName() {
        return drugStoreName;
    }

    public void setDrugStoreName(String drugStoreName) {
        this.drugStoreName = drugStoreName;
    }

    public String getDrugStoreAddr() {
        return drugStoreAddr;
    }

    public void setDrugStoreAddr(String drugStoreAddr) {
        this.drugStoreAddr = drugStoreAddr;
    }

    public BigDecimal getDecoctionFee() {
        return decoctionFee;
    }

    public void setDecoctionFee(BigDecimal decoctionFee) {
        this.decoctionFee = decoctionFee;
    }

    public BigDecimal getDecoctionUnitPrice() {
        return decoctionUnitPrice;
    }

    public void setDecoctionUnitPrice(BigDecimal decoctionUnitPrice) {
        this.decoctionUnitPrice = decoctionUnitPrice;
    }

    public Integer getCopyNum() {
        return copyNum;
    }

    public void setCopyNum(Integer copyNum) {
        this.copyNum = copyNum;
    }

    public String getCompleteAddress() {
        return completeAddress;
    }

    public void setCompleteAddress(String completeAddress) {
        this.completeAddress = completeAddress;
    }

    public String getRecipeMode() {
        return recipeMode;
    }

    public void setRecipeMode(String recipeMode) {
        this.recipeMode = recipeMode;
    }

    public Integer getGiveMode() {
        return giveMode;
    }

    public void setGiveMode(Integer giveMode) {
        this.giveMode = giveMode;
    }

    public String getEnterpriseName() {
        return enterpriseName;
    }

    public void setEnterpriseName(String enterpriseName) {
        this.enterpriseName = enterpriseName;
    }

    public BigDecimal getTcmFee() {
        return tcmFee;
    }

    public void setTcmFee(BigDecimal tcmFee) {
        this.tcmFee = tcmFee;
    }
}
