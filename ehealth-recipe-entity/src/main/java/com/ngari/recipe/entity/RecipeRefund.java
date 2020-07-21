package com.ngari.recipe.entity;

import ctd.account.session.ClientSession;
import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.FileToken;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author gaomw
 */
@Entity
@Schema
@Table(name = "cdr_recipe_refund")
@Access(AccessType.PROPERTY)
public class RecipeRefund implements Serializable {

    private static final long serialVersionUID = -6170665419368031590L;

    @ItemProperty(alias = "主键ID")
    private Integer id;

    @ItemProperty(alias = "处方序号")
    private Integer busId;

    @ItemProperty(alias = "医院Id")
    private Integer  organId;

    @ItemProperty(alias = "患者Id")
    private String mpiid;

    @ItemProperty(alias = "患者Id")
    private String patientName;

    @ItemProperty(alias = "审核医生id")
    private Integer doctorId;

    @ItemProperty(alias = "支付流水号")
    private String tradeNo;

    @ItemProperty(alias = "订单金额")
    private Double price;

    @ItemProperty(alias = "退费申请序号")
    private String applyNo;

    @ItemProperty(alias = "当前节点")
    private Integer node;

    @ItemProperty(alias = "状态")
    private Integer status;

    @ItemProperty(alias = "申请审核理由")
    private String reason;

    @ItemProperty(alias = "审核时间")
    private Date checkTime;

    @ItemProperty(alias = "审核时间")
    private Date applyTime;

    @ItemProperty(alias = "前一节点")
    private Integer beforeNode;

    @ItemProperty(alias = "备注")
    private String memo;

    @ItemProperty(alias = "预留（后面要临时存扩展字段可以用键值对存）")
    private String expand;

    public RecipeRefund() {
    }
    public RecipeRefund(Integer recipeId, String patientName, String reason, Double price, int status, Date checkTime) {
        this.busId = recipeId;
        this.patientName = patientName;
        this.reason = reason;
        this.price = price;
        this.status = status;
        this.checkTime = checkTime;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    public Integer getId() {
        return id;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBusId() {
        return busId;
    }

    public void setBusId(Integer busId) {
        this.busId = busId;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    public String getApplyNo() {
        return applyNo;
    }

    public void setApplyNo(String applyNo) {
        this.applyNo = applyNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getNode() {
        return node;
    }

    public void setNode(Integer node) {
        this.node = node;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(Date applyTime) {
        this.applyTime = applyTime;
    }

    public Date getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(Date checkTime) {
        this.checkTime = checkTime;
    }

    public Integer getBeforeNode() {
        return beforeNode;
    }

    public void setBeforeNode(Integer beforeNode) {
        this.beforeNode = beforeNode;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getExpand() {
        return expand;
    }

    public void setExpand(String expand) {
        this.expand = expand;
    }

    public String getMpiid() {
        return mpiid;
    }

    public void setMpiid(String mpiid) {
        this.mpiid = mpiid;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
