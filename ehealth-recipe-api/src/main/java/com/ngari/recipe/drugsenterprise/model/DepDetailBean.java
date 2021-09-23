package com.ngari.recipe.drugsenterprise.model;

import ctd.schema.annotation.Schema;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2017/7/3.
 */
@Schema
public class DepDetailBean {

    private Integer depId;

    private String depName;

    private Integer payMode;

    /**
     *  给药方式文案显示
     */
    private String giveModeText;

    /**
     *  购药方式文案显示
     */
    private String payModeText;

    /**
     * 处方费
     */
    private BigDecimal recipeFee;

    /**
     * 配送费
     */
    private BigDecimal expressFee;

    private String unSendTitle;

    /**
     * 以下为钥世圈字段，跳转链接时需要带上
     */
    private String gysCode;

    /**
     * sendMethod    0：送货上门   1：到店取药  2：两者皆可
     */
    private String sendMethod;

    /**
     * payMethod     0：线下支付   1：在线支付  2：两者皆可
     */
    private String payMethod;

    //以下为标准接口返回字段，之前字段不做处理

    /**
     * 如有药店则使用药店编码，区分 depId
     */
    private String pharmacyCode;

    /**
     * 自费金额
     */
    private BigDecimal actualFee;

    /**
     * 优惠金额
     */
    private BigDecimal couponFee;

    /**
     * 待煎费用
     */
    private BigDecimal decoctionFee;

    /**
     * 医保报销金额
     */
    private BigDecimal medicalFee;

    /**
     * 药店地址
     */
    private String address;

    //药店坐标
    private Position position;

    //药店所属药企名称
    private String belongDepName;

    //距离
    private Double distance;

    //是否跳转第三方
    private Integer orderType;

    //是否是还是返回的药企
    private Boolean hisDep;

    //his的药企code
    private String hisDepCode;

    //his的药企处方金额
    private BigDecimal hisDepFee;

    //药店|药柜 区分类型
    private Integer type;

    public BigDecimal getHisDepFee() {
        return hisDepFee;
    }

    public void setHisDepFee(BigDecimal hisDepFee) {
        this.hisDepFee = hisDepFee;
    }

    public String getHisDepCode() {
        return hisDepCode;
    }

    public void setHisDepCode(String hisDepCode) {
        this.hisDepCode = hisDepCode;
    }

    public Boolean getHisDep() {
        return hisDep;
    }

    public void setHisDep(Boolean hisDep) {
        this.hisDep = hisDep;
    }

    //药企备注
    private String memo;

    public Integer getDepId() {
        return depId;
    }

    public void setDepId(Integer depId) {
        this.depId = depId;
    }

    public String getDepName() {
        return depName;
    }

    public void setDepName(String depName) {
        this.depName = depName;
    }

    public Integer getPayMode() {
        return payMode;
    }

    public void setPayMode(Integer payMode) {
        this.payMode = payMode;
    }

    public String getGiveModeText() {
        return giveModeText;
    }

    public void setGiveModeText(String giveModeText) {
        this.giveModeText = giveModeText;
    }

    public BigDecimal getRecipeFee() {
        return recipeFee;
    }

    public void setRecipeFee(BigDecimal recipeFee) {
        this.recipeFee = recipeFee;
    }

    public BigDecimal getExpressFee() {
        return expressFee;
    }

    public void setExpressFee(BigDecimal expressFee) {
        this.expressFee = expressFee;
    }

    public String getUnSendTitle() {
        return unSendTitle;
    }

    public void setUnSendTitle(String unSendTitle) {
        this.unSendTitle = unSendTitle;
    }

    public String getGysCode() {
        return gysCode;
    }

    public void setGysCode(String gysCode) {
        this.gysCode = gysCode;
    }

    public String getSendMethod() {
        return sendMethod;
    }

    public void setSendMethod(String sendMethod) {
        this.sendMethod = sendMethod;
    }

    public String getPayMethod() {
        return payMethod;
    }

    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
    }

    public String getPharmacyCode() {
        return pharmacyCode;
    }

    public void setPharmacyCode(String pharmacyCode) {
        this.pharmacyCode = pharmacyCode;
    }

    public BigDecimal getActualFee() {
        return actualFee;
    }

    public void setActualFee(BigDecimal actualFee) {
        this.actualFee = actualFee;
    }

    public BigDecimal getCouponFee() {
        return couponFee;
    }

    public void setCouponFee(BigDecimal couponFee) {
        this.couponFee = couponFee;
    }

    public BigDecimal getDecoctionFee() {
        return decoctionFee;
    }

    public void setDecoctionFee(BigDecimal decoctionFee) {
        this.decoctionFee = decoctionFee;
    }

    public BigDecimal getMedicalFee() {
        return medicalFee;
    }

    public void setMedicalFee(BigDecimal medicalFee) {
        this.medicalFee = medicalFee;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public String getBelongDepName() {
        return belongDepName;
    }

    public void setBelongDepName(String belongDepName) {
        this.belongDepName = belongDepName;
    }

    public String getPayModeText() {
        return payModeText;
    }

    public void setPayModeText(String payModeText) {
        this.payModeText = payModeText;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Integer getOrderType() {
        return orderType;
    }

    public void setOrderType(Integer orderType) {
        this.orderType = orderType;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DepDetailBean that = (DepDetailBean) o;
        return Objects.equals(depId, that.depId) && Objects.equals(depName, that.depName) && Objects.equals(payMode, that.payMode) && Objects.equals(giveModeText, that.giveModeText) && Objects.equals(payModeText, that.payModeText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(depId, depName, payMode, giveModeText, payModeText);
    }
}
