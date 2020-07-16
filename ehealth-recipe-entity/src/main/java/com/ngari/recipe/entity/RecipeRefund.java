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
 * @author yuyun
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
    private String  organId;

    @ItemProperty(alias = "操作人员Id（患者Id、医生Id、医院默认为'his'）")
    private String userId;

    @ItemProperty(alias = "操作人类型（1：患者，2：医生，3：医院）")
    private Integer userType;

    @ItemProperty(alias = "支付流水号")
    private String tradeNo;

    @ItemProperty(alias = "退费申请序号")
    private String applyNo;

    @ItemProperty(alias = "当前节点")
    private Integer node;

    @ItemProperty(alias = "状态")
    private Integer status;

    @ItemProperty(alias = "申请审核理由")
    private String reason;

    @ItemProperty(alias = "修改时间")
    private Date modifyDate;

    @ItemProperty(alias = "前一节点")
    private Integer beforeNode;

    @ItemProperty(alias = "备注")
    private String memo;

    @ItemProperty(alias = "预留（后面要临时存扩展字段可以用键值对存）")
    private String expand;

    public Integer getId() {
        return id;
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

    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getUserType() {
        return userType;
    }

    public void setUserType(Integer userType) {
        this.userType = userType;
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

    public Integer getBeforeNode() {
        return beforeNode;
    }

    public void setBeforeNode(Integer beforeNode) {
        this.beforeNode = beforeNode;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
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
}
