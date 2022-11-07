package com.ngari.recipe.entity;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Schema
@Table(name = "cdr_before_order")
@Access(AccessType.PROPERTY)
public class RecipeBeforeOrder implements Serializable {

    private static final long serialVersionUID = 4184345796220351836L;

    @ItemProperty(alias = "主键ID")
    private Integer id;

    @ItemProperty(alias = "机构ID")
    private Integer organId;

    @ItemProperty(alias = "处方编码")
    private String recipeCode;

    @ItemProperty(alias = "处方单号")
    private Integer recipeId;

    @ItemProperty(alias = "药企ID")
    private Integer enterpriseId;

    @ItemProperty(alias = "取药药店或站点名称")
    private String drugStoreName;

    @ItemProperty(alias = "取药药店或站点编码")
    private String drugStoreCode;

    @ItemProperty(alias = "取药药店或站点地址")
    private String drugStoreAddr;

    @ItemProperty(alias = "配送地址id")
    private Integer addressId;

    @ItemProperty(alias = "完整地址")
    private String completeAddress;

    @ItemProperty(alias = "收货人")
    @Desensitizations(type = DesensitizationsType.NAME)
    private String receiver;

    @ItemProperty(alias = "收货人手机号")
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String recMobile;

    @ItemProperty(alias = "收货人电话")
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String recTel;

    @ItemProperty(alias = "地址（省）")
    private String address1;

    @ItemProperty(alias = "地址（市）")
    private String address2;

    @ItemProperty(alias = "地址（区）")
    private String address3;

    @ItemProperty(alias = "地址（街道）")
    private String streetAddress;

    @ItemProperty(alias = "详细地址")
    private String address4;

    @ItemProperty(alias = "社区编码")
    private String address5;

    @ItemProperty(alias = "社区名称")
    private String address5Text;

    @ItemProperty(alias = "邮政编码")
    private String zipCode;

    @ItemProperty(alias = "是否已完善  0 否，1 是")
    private Integer isReady;

    @ItemProperty(alias = "删除标识，0：正常，1：删除")
    private Integer deleteFlag;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "最后修改时间")
    private Date updateTime;

    @ItemProperty(alias = "购药方式")
    private Integer giveMode;

    @ItemProperty(alias = "支付方式")
    private String payWay;

    @ItemProperty(alias = "操作人mpiId")
    private String operMpiId;

    @ItemProperty(alias = "0 无 1 药店取药，2 站点取药")
    private Integer takeMedicineWay;

    @ItemProperty(alias = "订单所属配送方式")
    private String giveModeKey;

    @ItemProperty(alias = "患者购药方式文本")
    private String giveModeText;

    @ItemProperty(alias = "配送费")
    private BigDecimal expressFee;

    @ItemProperty(alias = "代煎费")
    private BigDecimal decoctionFee;

    @ItemProperty(alias = "中医辨证论治费")
    private BigDecimal tcmFee;

    @ItemProperty(alias = "审方费")
    private BigDecimal auditFee;

    @ItemProperty(alias = "处方费")
    private BigDecimal recipeFee;

    @ItemProperty(alias = "是否已锁定  0 否，1 是")
    private Integer isLock;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "organ_id")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "recipe_code")
    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    @Column(name = "recipe_id")
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Column(name = "enterprise_id")
    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    @Column(name = "drug_store_name")
    public String getDrugStoreName() {
        return drugStoreName;
    }

    public void setDrugStoreName(String drugStoreName) {
        this.drugStoreName = drugStoreName;
    }

    @Column(name = "drug_store_code")
    public String getDrugStoreCode() {
        return drugStoreCode;
    }

    public void setDrugStoreCode(String drugStoreCode) {
        this.drugStoreCode = drugStoreCode;
    }

    @Column(name = "drug_store_addr")
    public String getDrugStoreAddr() {
        return drugStoreAddr;
    }

    public void setDrugStoreAddr(String drugStoreAddr) {
        this.drugStoreAddr = drugStoreAddr;
    }

    @Column(name = "address_id")
    public Integer getAddressId() {
        return addressId;
    }

    public void setAddressId(Integer addressId) {
        this.addressId = addressId;
    }

    @Column(name = "receiver")
    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    @Column(name = "rec_mobile")
    public String getRecMobile() {
        return recMobile;
    }

    public void setRecMobile(String recMobile) {
        this.recMobile = recMobile;
    }

    @Column(name = "rec_tel")
    public String getRecTel() {
        return recTel;
    }

    public void setRecTel(String recTel) {
        this.recTel = recTel;
    }

    @Column(name = "address1")
    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    @Column(name = "address2")
    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    @Column(name = "address3")
    public String getAddress3() {
        return address3;
    }

    public void setAddress3(String address3) {
        this.address3 = address3;
    }

    @Column(name = "address4")
    public String getAddress4() {
        return address4;
    }

    public void setAddress4(String address4) {
        this.address4 = address4;
    }

    @Column(name = "address5")
    public String getAddress5() {
        return address5;
    }

    public void setAddress5(String address5) {
        this.address5 = address5;
    }

    @Column(name = "address5_text")
    public String getAddress5Text() {
        return address5Text;
    }

    public void setAddress5Text(String address5Text) {
        this.address5Text = address5Text;
    }

    @Column(name = "zip_code")
    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    @Column(name = "is_ready")
    public Integer getIsReady() {
        return isReady;
    }

    public void setIsReady(Integer isReady) {
        this.isReady = isReady;
    }

    @Column(name = "delete_flag")
    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    @Column(name = "create_time")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "update_time")
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Column(name = "give_mode")
    public Integer getGiveMode() {
        return giveMode;
    }

    public void setGiveMode(Integer giveMode) {
        this.giveMode = giveMode;
    }

    @Column(name = "pay_way")
    public String getPayWay() {
        return payWay;
    }

    public void setPayWay(String payWay) {
        this.payWay = payWay;
    }

    @Column(name = "oper_mpiId")
    public String getOperMpiId() {
        return operMpiId;
    }

    public void setOperMpiId(String operMpiId) {
        this.operMpiId = operMpiId;
    }

    @Column(name = "take_medicine_way")
    public Integer getTakeMedicineWay() {
        return takeMedicineWay;
    }

    public void setTakeMedicineWay(Integer takeMedicineWay) {
        this.takeMedicineWay = takeMedicineWay;
    }

    @Column(name = "give_mode_key")
    public String getGiveModeKey() {
        return giveModeKey;
    }

    public void setGiveModeKey(String giveModeKey) {
        this.giveModeKey = giveModeKey;
    }

    @Column(name = "give_mode_text")
    public String getGiveModeText() {
        return giveModeText;
    }

    public void setGiveModeText(String giveModeText) {
        this.giveModeText = giveModeText;
    }

    @Column(name = "express_fee")
    public BigDecimal getExpressFee() {
        return expressFee;
    }

    public void setExpressFee(BigDecimal expressFee) {
        this.expressFee = expressFee;
    }

    @Column(name = "decoction_fee")
    public BigDecimal getDecoctionFee() {
        return decoctionFee;
    }

    public void setDecoctionFee(BigDecimal decoctionFee) {
        this.decoctionFee = decoctionFee;
    }

    @Column(name = "tcm_fee")
    public BigDecimal getTcmFee() {
        return tcmFee;
    }

    public void setTcmFee(BigDecimal tcmFee) {
        this.tcmFee = tcmFee;
    }

    @Column(name = "audit_fee")
    public BigDecimal getAuditFee() {
        return auditFee;
    }

    public void setAuditFee(BigDecimal auditFee) {
        this.auditFee = auditFee;
    }

    @Column(name = "recipe_fee")
    public BigDecimal getRecipeFee() {
        return recipeFee;
    }

    public void setRecipeFee(BigDecimal recipeFee) {
        this.recipeFee = recipeFee;
    }

    @Column(name = "street_address")
    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    @Column(name = "complete_address")
    public String getCompleteAddress() {
        return completeAddress;
    }

    public void setCompleteAddress(String completeAddress) {
        this.completeAddress = completeAddress;
    }

    @Column(name = "is_lock")
    public Integer getIsLock() {
        return isLock;
    }

    public void setIsLock(Integer isLock) {
        this.isLock = isLock;
    }
}
