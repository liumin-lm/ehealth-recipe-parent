package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目目录
 * @author yinsheng
 * @date 2021\8\20 0020 16:28
 */
@Entity
@Schema
@Table(name = "base_item_list")
@Access(AccessType.PROPERTY)
public class ItemList implements Serializable{
    private static final long serialVersionUID = 7143045971223592496L;
    @ItemProperty(alias = "项目id")
    private Integer id;
    @ItemProperty(alias = "机构id")
    private Integer organID;
    @ItemProperty(alias = "项目名称")
    private String itemName;
    @ItemProperty(alias = "项目编码")
    private String itemCode;
    @ItemProperty(alias = "项目单位")
    private String itemUnit;
    @ItemProperty(alias = "项目费用")
    private BigDecimal itemPrice;
    @ItemProperty(alias = "项目状态")
    private Integer status;
    @ItemProperty(alias = "删除状态 0 未删除 1 删除")
    private Integer deleted;
    @ItemProperty(alias = "创建时间")
    private Date gmtCreate;
    @ItemProperty(alias = "修改时间")
    private Date gmtModified;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "item_name")
    public String getItemName() {
        return itemName;
    }


    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    @Column(name = "organID")
    public Integer getOrganID() {
        return organID;
    }

    public void setOrganID(Integer organID) {
        this.organID = organID;
    }

    @Column(name = "item_code")
    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    @Column(name = "item_unit")
    public String getItemUnit() {
        return itemUnit;
    }

    public void setItemUnit(String itemUnit) {
        this.itemUnit = itemUnit;
    }

    @Column(name = "item_price")
    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    @Column(name = "status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "is_deleted")
    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    @Column(name = "gmt_create")
    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    @Column(name = "gmt_modified")
    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }
}
