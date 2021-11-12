package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author liuyuang
 */
@Schema
@Entity
@Table(name = "cdr_shopping_drug")
@Access(AccessType.PROPERTY)
public class ShoppingDrug implements java.io.Serializable{
    private static final long serialVersionUID = -5172680534324191606L;

    @ItemProperty(alias = "主键")
    private Integer id;
    @ItemProperty(alias = "订单编号")
    private String orderCode;
    @ItemProperty(alias = "药品名称")
    private String drugName;
    @ItemProperty(alias = "药品规格")
    private String drugSpec;
    @ItemProperty(alias = "药品生产厂家")
    private String producer;
    @ItemProperty(alias = "药品单价")
    private BigDecimal price;
    @ItemProperty(alias = "药品数量")
    private BigDecimal quantity;
    @ItemProperty(alias = "创建时间")
    private Date createTime;
    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    public ShoppingDrug() {
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "orderCode")
    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    @Column(name = "drugName")
    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    @Column(name = "drugSpec")
    public String getDrugSpec() {
        return drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    @Column(name = "producer")
    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    @Column(name = "price")
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Column(name = "quantity")
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    @Column(name = "createTime")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "lastModify")
    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModifyTime) {
        this.lastModify = lastModifyTime;
    }
}
