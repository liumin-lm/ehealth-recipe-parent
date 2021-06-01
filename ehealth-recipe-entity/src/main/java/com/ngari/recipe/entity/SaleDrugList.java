package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 销售机构药品目录
 *
 * @author <a href="mailto:luf@ngarihealth.com">luf</a>
 */
@Entity
@Schema
@Table(name = "base_saledruglist")
@Access(AccessType.PROPERTY)
public class SaleDrugList implements java.io.Serializable {
    private static final long serialVersionUID = -7090271704460035622L;

    @ItemProperty(alias = "机构药品序号")
    private Integer organDrugId;

    @ItemProperty(alias = "销售机构代码")
    private Integer organId;

    @ItemProperty(alias = "药品序号")
    private Integer drugId;

    @ItemProperty(alias = "机构药品编码")
    private String organDrugCode;

    @ItemProperty(alias = "机构药品名称")
    private String drugName;

    @ItemProperty(alias = "商品名称")
    private String saleName;

    @ItemProperty(alias = "机构药品规格")
    private String drugSpec;

    @ItemProperty(alias = "库存")
    private BigDecimal inventory;

    @ItemProperty(alias = "无税单价")
    private BigDecimal price;

    @ItemProperty(alias = "税率")
    private Double rate;

    @ItemProperty(alias = "含税单价")
    private Double ratePrice;

    @ItemProperty(alias = "使用状态")
    @Dictionary(id = "eh.base.dictionary.OrganDrugStatus")
    private Integer status;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "禁用原因")
    private String disableReason;

    public SaleDrugList() {
    }

    public SaleDrugList(Integer organDrugId) {
        this.organDrugId = organDrugId;
    }

    public SaleDrugList(Integer organDrugId, Integer organId,
                        Integer drugId, String organDrugCode, Double price, Double rate,
                        Double ratePrice, Integer status) {
        this.organDrugId = organDrugId;
        this.organId = organId;
        this.drugId = drugId;
        this.organDrugCode = organDrugCode;
        this.price = BigDecimal.valueOf(price);
        this.rate = rate;
        this.ratePrice = ratePrice;
        this.status = status;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "OrganDrugId", unique = true, nullable = false)
    public Integer getOrganDrugId() {
        return this.organDrugId;
    }

    public void setOrganDrugId(Integer organDrugId) {
        this.organDrugId = organDrugId;
    }

    @Column(name = "OrganID")
    public Integer getOrganId() {
        return this.organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "DrugId")
    public Integer getDrugId() {
        return this.drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    @Column(name = "OrganDrugCode", length = 30)
    public String getOrganDrugCode() {
        return this.organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    @Column(name = "drugName", length = 64)
    public String getDrugName() {
        return drugName;
    }

    @Column(name = "saleName", length = 64)
    public String getSaleName() {
        return saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    @Column(name = "drugSpec", length = 64)
    public String getDrugSpec() {
        return drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    @Column(name = "Inventory")
    public BigDecimal getInventory() {
        return inventory;
    }

    public void setInventory(BigDecimal inventory) {
        this.inventory = inventory;
    }

    @Column(name = "Price", precision = 10)
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Column(name = "Rate", precision = 10, scale = 4)
    public Double getRate() {
        return this.rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    @Column(name = "RatePrice", precision = 10)
    public Double getRatePrice() {
        return this.ratePrice;
    }

    public void setRatePrice(Double ratePrice) {
        this.ratePrice = ratePrice;
    }

    @Column(name = "Status")
    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "CreateDt", length = 19)
    public Date getCreateDt() {
        return this.createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
    }

    @Column(name = "LastModify", length = 19)
    public Date getLastModify() {
        return this.lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    @Column(name = "disableReason")
    public String getDisableReason() {
        return disableReason;
    }

    public void setDisableReason(String disableReason) {
        this.disableReason = disableReason;
    }
}