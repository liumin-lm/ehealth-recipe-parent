package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by  on 2017/5/23.
 * @author jiangtingfeng
 */
@Schema
@Entity
@Table(name = "cdr_commonRecipeDrug")
@Access(AccessType.PROPERTY)
public class CommonRecipeDrug implements java.io.Serializable{

    private static final long serialVersionUID = -5511208206035918004L;

    @ItemProperty(alias="自增id")
    private Integer id;

    @ItemProperty(alias="药品状态")
    private Integer drugStatus;

    @ItemProperty(alias="常用方Id")
    private Integer commonRecipeId;

    @ItemProperty(alias="药品ID")
    private Integer drugId;

    @ItemProperty(alias="药物名称")
    private String drugName;

    @ItemProperty(alias="药物单位")
    private String drugUnit;

    @ItemProperty(alias="药物规格")
    private String drugSpec;

    @ItemProperty(alias="药物使用总数量")
    private Double useTotalDose;

    @ItemProperty(alias="药物使用次剂量")
    private Double useDose;

    @ItemProperty(alias="默认每次剂量")
    private Double defaultUseDose;

    @ItemProperty(alias="销售价格")
    private BigDecimal salePrice;

    @ItemProperty(alias="销售单价")
    private Double price1;

    @ItemProperty(alias="总药物金额")
    private BigDecimal drugCost;

    @ItemProperty(alias="备注信息")
    private String memo;

    @ItemProperty(alias="创建时间")
    private Date createDt;

    @ItemProperty(alias="最后修改时间")
    private Date lastModify;

    @ItemProperty(alias="药物使用频率代码")
    @Dictionary(id="eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias="药物使用途径代码")
    @Dictionary(id="eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias="药物使用天数")
    private Integer useDays;

    @ItemProperty(alias="剂量单位")
    private String useDoseUnit;

    @Column(name = "UseDoseUnit")
    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    @Transient
    public Integer getDrugStatus() {
        return drugStatus;
    }

    public void setDrugStatus(Integer drugStatus) {
        this.drugStatus = drugStatus;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id" ,nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "UsePathways")
    public String getUsePathways() {
        return usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    @Column(name = "DefaultUseDose")
    public Double getDefaultUseDose() {
        return defaultUseDose;
    }

    public void setDefaultUseDose(Double defaultUseDose) {
        this.defaultUseDose = defaultUseDose;
    }

    @Column(name = "UseDays")
    public Integer getUseDays() {
        return useDays;
    }

    public void setUseDays(Integer useDays) {
        this.useDays = useDays;
    }

    @Column(name = "DrugName")
    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    @Column(name = "DrugUnit")
    public String getDrugUnit() {
        return drugUnit;
    }

    public void setDrugUnit(String drugUnit) {
        this.drugUnit = drugUnit;
    }

    @Column(name = "DrugSpec")
    public String getDrugSpec() {
        return drugSpec;
    }

    @Column(name = "DrugSpec")
    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    @Column(name = "UseTotalDose")
    public Double getUseTotalDose() {
        return useTotalDose;
    }

    public void setUseTotalDose(Double useTotalDose) {
        this.useTotalDose = useTotalDose;
    }

    @Column(name = "UseDose")
    public Double getUseDose() {
        return useDose;
    }

    public void setUseDose(Double useDose) {
        this.useDose = useDose;
    }

    @Column(name = "SalePrice")
    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    @Transient
    public Double getPrice1() {
        return price1;
    }

    public void setPrice1(Double price1) {
        this.price1 = price1;
    }

    @Column(name = "DrugCost")
    public BigDecimal getDrugCost() {
        return drugCost;
    }

    public void setDrugCost(BigDecimal drugCost) {
        this.drugCost = drugCost;
    }

    @Column(name = "Memo")
    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Column(name = "CreateDt")
    public Date getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
    }

    @Column(name = "LastModify")
    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    @Column(name = "UsingRate")
    public String getUsingRate() {
        return usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    @Column(name = "CommonRecipeId")
    public Integer getCommonRecipeId() {
        return commonRecipeId;
    }

    public void setCommonRecipeId(Integer commonRecipeId) {
        this.commonRecipeId = commonRecipeId;
    }
    @Column(name = "DrugId")
    public Integer getDrugId() {
        return drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    @Override
    public String toString() {
        return "CommonRecipeDrug{" +
                "id=" + id +
                ", commonRecipeId=" + commonRecipeId +
                ", drugId='" + drugId + '\'' +
                ", drugName='" + drugName + '\'' +
                ", drugUnit='" + drugUnit + '\'' +
                ", drugSpec='" + drugSpec + '\'' +
                ", useTotalDose=" + useTotalDose +
                ", useDose=" + useDose +
                ", defaultUseDose=" + defaultUseDose +
                ", salePrice=" + salePrice +
                ", drugCost=" + drugCost +
                ", memo='" + memo + '\'' +
                ", createDt=" + createDt +
                ", lastModify=" + lastModify +
                ", usingRate='" + usingRate + '\'' +
                ", usePathways='" + usePathways + '\'' +
                ", useDays=" + useDays +
                '}';
    }
}
