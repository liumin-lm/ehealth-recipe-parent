package com.ngari.recipe.shoppingorder.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author liuyuang
 */
@Schema
public class ShoppingDrugDTO implements java.io.Serializable {

    private static final long serialVersionUID = 1738152442084719014L;

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

    public ShoppingDrugDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getDrugSpec() {
        return drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }
}
