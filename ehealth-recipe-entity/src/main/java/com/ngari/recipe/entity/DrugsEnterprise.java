package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.FileToken;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.util.Date;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 药企
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/6.
 */

@Entity
@Schema
@Table(name = "cdr_drugsenterprise")
@Access(AccessType.PROPERTY)
public class DrugsEnterprise implements java.io.Serializable {

    private static final long serialVersionUID = 7806649469165719455L;

    @ItemProperty(alias = "药企序号")
    private Integer id;

    @ItemProperty(alias = "药企名称")
    private String name;

    @ItemProperty(alias = "药企固定编码")
    private String enterpriseCode;

    @ItemProperty(alias = "药企分配appKey从开放平台获取")
    private String appKey;

    @ItemProperty(alias = "药企在平台的账户")
    private String account;

    @ItemProperty(alias = "用户名")
    private String userId;

    @ItemProperty(alias = "密码")
    private String password;

    @ItemProperty(alias = "药企联系电话")
    private String tel;

    @ItemProperty(alias = "药企实现类简称，默认使用 common， 也就是国药的一套实现")
    private String callSys;

    @ItemProperty(alias = "结算方式 0:药店价格 1:医院价格")
    private Integer settlementMode;

    @ItemProperty(alias = "调用接口标识")
    private String token;

    @ItemProperty(alias = "药企平台鉴权地址")
    private String authenUrl;

    @ItemProperty(alias = "药企平台业务处理地址")
    private String businessUrl;

    @ItemProperty(alias = "创建时间")
    private Date createDate;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "配送模式支持 0:不支持 1:线上付款 2:货到付款 3:药店取药 8:货到付款和药店取药 9:都支持")
    private Integer payModeSupport;

    @ItemProperty(alias = "院内补充库存 0:非补充，1：补充库存")
    private Integer hosInteriorSupport;

    @ItemProperty(alias = "提交订单的类型0 提交订单到第三方 1 系统提交")
    private Integer orderType;

    @ItemProperty(alias = "状态标识")
    private Integer status;

    @ItemProperty(alias = "排序，1最前，越往后越小")
    private Integer sort;

    @ItemProperty(alias = "校验药品库存标志0 不需要校验 1 需要校验")
    private Integer checkInventoryFlag;

    @ItemProperty(alias = "创建类型：1：非自建  0：自建")
    @Dictionary(id = "eh.cdr.dictionary.DepType")
    private Integer createType;

    @ItemProperty(alias = "运费细则图片ID")
    @FileToken(expires = 3600)
    private String transFeeDetail;

    @ItemProperty(alias = "是否医院类型药企：1医院结算药企，0普通药企")
    private Integer isHosDep;

    @ItemProperty(alias = "药企备注")
    private String memo;

    @ItemProperty(alias = "是否支持省直医保：1不支持，0支持 默认0")
    private Integer medicalInsuranceSupport;

    @ItemProperty(alias = "0:不支付药品费用，1:全部支付")
    private Integer storePayFlag;

    @ItemProperty(alias = "配送主体类型 1 药企配送 2 医院配送")
    private Integer sendType;

    @ItemProperty(alias = "配送费支付方式 1-在线支付 2-线下支付 3-第三方支付")
    private Integer expressFeePayWay;

    @ItemProperty(alias = "管理单元")
    private String manageUnit;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "enterpriseCode")
    public String getEnterpriseCode() {
        return enterpriseCode;
    }

    public void setEnterpriseCode(String enterpriseCode) {
        this.enterpriseCode = enterpriseCode;
    }

    @Column(name = "appKey")
    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    @Column(name = "Account", length = 20)
    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    @Column(name = "UserId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Column(name = "Password")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column(name = "Tel")
    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    @Column(name = "CallSys")
    public String getCallSys() {
        return callSys;
    }

    public void setCallSys(String callSys) {
        this.callSys = callSys;
    }

    @Column(name = "settlementMode")
    public Integer getSettlementMode() {
        return settlementMode;
    }

    public void setSettlementMode(Integer settlementMode) {
        this.settlementMode = settlementMode;
    }

    @Column(name = "Token")
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Column(name = "AuthenUrl")
    public String getAuthenUrl() {
        return authenUrl;
    }

    public void setAuthenUrl(String authenUrl) {
        this.authenUrl = authenUrl;
    }

    @Column(name = "BusinessUrl")
    public String getBusinessUrl() {
        return businessUrl;
    }

    public void setBusinessUrl(String businessUrl) {
        this.businessUrl = businessUrl;
    }

    @Column(name = "CreateDate")
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column(name = "LastModify")
    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    @Column(name = "PayModeSupport")
    public Integer getPayModeSupport() {
        return payModeSupport;
    }

    public void setPayModeSupport(Integer payModeSupport) {
        this.payModeSupport = payModeSupport;
    }

    @Column(name = "hosInteriorSupport")
    public Integer getHosInteriorSupport() {
        return hosInteriorSupport;
    }

    public void setHosInteriorSupport(Integer hosInteriorSupport) {
        this.hosInteriorSupport = hosInteriorSupport;
    }

    @Column(name = "orderType")
    public Integer getOrderType() {
        return orderType;
    }

    public void setOrderType(Integer orderType) {
        this.orderType = orderType;
    }

    @Column(name = "Status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "Sort")
    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Column(name = "checkInventoryFlag")
    public Integer getCheckInventoryFlag() {
        return checkInventoryFlag;
    }

    public void setCheckInventoryFlag(Integer checkInventoryFlag) {
        this.checkInventoryFlag = checkInventoryFlag;
    }

    public Integer getCreateType() {
        return createType;
    }

    public void setCreateType(Integer createType) {
        this.createType = createType;
    }

    @Column(name = "TransFeeDetail")
    public String getTransFeeDetail() {
        return transFeeDetail;
    }

    public void setTransFeeDetail(String transFeeDetail) {
        this.transFeeDetail = transFeeDetail;
    }

    @Column(name = "isHosDep")
    public Integer getIsHosDep() {
        return isHosDep;
    }

    public void setIsHosDep(Integer isHosDep) {
        this.isHosDep = isHosDep;
    }

    @Column(name = "memo")
    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Column(name = "MedicalInsuranceSupport")
    public Integer getMedicalInsuranceSupport() {
        return medicalInsuranceSupport;
    }

    public void setMedicalInsuranceSupport(Integer medicalInsuranceSupport) {
        this.medicalInsuranceSupport = medicalInsuranceSupport;
    }

    @Column(name = "storePayFlag")
    public Integer getStorePayFlag() {
        return storePayFlag;
    }

    public void setStorePayFlag(Integer storePayFlag) {
        this.storePayFlag = storePayFlag;
    }

    @Column(name = "sendType")
    public Integer getSendType() {
        return sendType;
    }

    public void setSendType(Integer sendType) {
        this.sendType = sendType;
    }

    @Column(name = "expressFeePayWay")
    public Integer getExpressFeePayWay() {
        return expressFeePayWay;
    }

    public void setExpressFeePayWay(Integer expressFeePayWay) {
        this.expressFeePayWay = expressFeePayWay;
    }

    @Column(name = "manageUnit")
    public String getManageUnit() {
        return manageUnit;
    }

    public void setManageUnit(String manageUnit) {
        this.manageUnit = manageUnit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        DrugsEnterprise that = (DrugsEnterprise) o;

        if (!id.equals(that.id)) {return false;}

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
