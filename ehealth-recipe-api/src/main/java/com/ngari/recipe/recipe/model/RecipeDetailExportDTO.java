package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

import static javax.persistence.GenerationType.IDENTITY;

/**
 *@author  Created by liuxiaofeng on 2020/12/7.
 * 配送订单导出 处方实体
 */
@Schema
@Entity
@Access(AccessType.PROPERTY)
public class RecipeDetailExportDTO implements Serializable{
    private static final long serialVersionUID = 5319317470905380390L;

    @ItemProperty(alias="处方明细序号")
    private Integer recipeDetailId;
    @ItemProperty(alias="药物名称")
    private String drugName;
    @ItemProperty(alias="药物规格")
    private String drugSpec;
    @ItemProperty(alias="药物单位")
    private String drugUnit;
    @ItemProperty(alias="药物使用次剂量")
    private Double useDose;
    @ItemProperty(alias="药物使用规格单位")
    private String useDoseUnit;
    @ItemProperty(alias="平台药物使用频率代码")
    @Dictionary(id="eh.cdr.dictionary.UsingRate")
    private String usingRate;
    @ItemProperty(alias="平台药物使用途径代码")
    @Dictionary(id="eh.cdr.dictionary.UsePathways")
    private String usePathways;
    @ItemProperty(alias="销售价格")
    private BigDecimal salePrice;
    //同步机构药品信息新添字段
    @ItemProperty(alias = "生产厂家")
    private String producer;
    //同步机构药品信息新添字段
    @ItemProperty(alias = "批准文号")
    private String licenseNumber;
    @ItemProperty(alias = "实际销售价格")
    private BigDecimal actualSalePrice;
    @ItemProperty(alias = "药企药品编码")
    private String saleDrugCode;
    @ItemProperty(alias="药物使用总数量")
    private Double useTotalDose;


    @Column(name = "licenseNumber")
    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }


    @Column(name = "producer")
    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "RecipeDetailID", unique = true, nullable = false)
    public Integer getRecipeDetailId() {
        return this.recipeDetailId;
    }

    public void setRecipeDetailId(Integer recipeDetailId) {
        this.recipeDetailId = recipeDetailId;
    }


    @Column(name = "DrugName", nullable = false, length = 50)
    public String getDrugName() {
        return this.drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    @Column(name = "DrugSpec", nullable = false, length = 30)
    public String getDrugSpec() {
        return this.drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }



    @Column(name = "DrugUnit")
    public String getDrugUnit() {
        return this.drugUnit;
    }

    public void setDrugUnit(String drugUnit) {
        this.drugUnit = drugUnit;
    }

    @Column(name = "UseDose")
    public Double getUseDose() {
        return this.useDose;
    }

    public void setUseDose(Double useDose) {
        this.useDose = useDose;
    }


    @Column(name = "UseDoseUnit", length = 6)
    public String getUseDoseUnit() {
        return this.useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }



    @Column(name = "UsingRate", length = 20)
    public String getUsingRate() {
        return this.usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    @Column(name = "UsePathways", length = 20)
    public String getUsePathways() {
        return this.usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    @Column(name = "UseTotalDose", precision = 10, scale = 3)
    public Double getUseTotalDose() {
        return this.useTotalDose;
    }

    public void setUseTotalDose(Double useTotalDose) {
        this.useTotalDose = useTotalDose;
    }



    @Column(name = "salePrice", precision = 10)
    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }



    @Column(name = "actualSalePrice")
    public BigDecimal getActualSalePrice() {
        return actualSalePrice;
    }

    public void setActualSalePrice(BigDecimal actualSalePrice) {
        this.actualSalePrice = actualSalePrice;
    }

    @Column(name = "saleDrugCode")
    public String getSaleDrugCode() {
        return saleDrugCode;
    }

    public void setSaleDrugCode(String saleDrugCode) {
        this.saleDrugCode = saleDrugCode;
    }

}
