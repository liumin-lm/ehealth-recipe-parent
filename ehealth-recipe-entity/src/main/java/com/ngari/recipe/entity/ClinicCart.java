package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @Description
 * @Author yzl
 * @Date 2022-07-14
 */
@Schema
@Entity
@Table(name = "cdr_clinic_cart")
@Access(AccessType.PROPERTY)
public class ClinicCart implements Serializable {

    private static final long serialVersionUID = -3823510318896652272L;

    @ItemProperty(alias = "主键Id")
    private Integer id;

    @ItemProperty(alias = "机构Id")
    private Integer organId;

    @ItemProperty(alias = "操作人员Id")
    private String userId;

    @ItemProperty(alias = "项目Id，处方organDrugCode, 检验检查ItemId")
    private String itemId;

    @ItemProperty(alias = "项目名称，处方：药品名称，检验检查：项目名称")
    private String itemName;

    @ItemProperty(alias = "项目详情说明")
    private String itemDetail;

    @ItemProperty(alias = "项目类型：1：药品，2：检查，3：检验")
    private Integer itemType;

    @ItemProperty(alias = "项目数量，处方为药品数量")
    private Integer amount;

    @ItemProperty(alias = "项目数量单位")
    private String unit;

    @ItemProperty(alias = "删除标识，0：正常，1：删除")
    private Integer deleteFlag;

    @ItemProperty(alias = "业务场景, 方便门诊:1, 便捷购药:2")
    private Integer workType;

    @ItemProperty(alias = "项目价格")
    private BigDecimal itemPrice;


    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
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

    @Column(name = "user_id")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Column(name = "item_id")
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    @Column(name = "item_name")
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    @Column(name = "item_detail")
    public String getItemDetail() {
        return itemDetail;
    }

    public void setItemDetail(String itemDetail) {
        this.itemDetail = itemDetail;
    }

    @Column(name = "item_type")
    public Integer getItemType() {
        return itemType;
    }

    public void setItemType(Integer itemType) {
        this.itemType = itemType;
    }

    @Column(name = "amount")
    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    @Column(name = "unit")
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Column(name = "delete_flag")
    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    @Column(name = "work_type")
    public Integer getWorkType() {
        return workType;
    }

    public void setWorkType(Integer workType) {
        this.workType = workType;
    }

    @Column(name = "item_price")
    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }
}
