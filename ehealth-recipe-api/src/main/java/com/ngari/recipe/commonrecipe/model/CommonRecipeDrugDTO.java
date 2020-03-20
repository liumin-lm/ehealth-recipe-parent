package com.ngari.recipe.commonrecipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by  on 2017/5/23.
 *
 * @author jiangtingfeng
 */
@Schema
public class CommonRecipeDrugDTO implements java.io.Serializable {

    private static final long serialVersionUID = -4535607360492071383L;

    @ItemProperty(alias = "自增id")
    private Integer id;

    @ItemProperty(alias = "药品状态")
    private Integer drugStatus;

    @ItemProperty(alias = "常用方Id")
    private Integer commonRecipeId;

    @ItemProperty(alias = "药品ID")
    private Integer drugId;

    @ItemProperty(alias = "机构药品编码")
    private String organDrugCode;

    @ItemProperty(alias = "药物名称")
    private String drugName;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias = "药物单位")
    private String drugUnit;

    @ItemProperty(alias = "药物规格")
    private String drugSpec;

    @ItemProperty(alias = "药物使用总数量")
    private Double useTotalDose;

    @ItemProperty(alias = "药物使用次剂量")
    private Double useDose;

    @ItemProperty(alias = "默认每次剂量")
    private Double defaultUseDose;

    @ItemProperty(alias = "销售价格")
    private BigDecimal salePrice;

    @ItemProperty(alias = "销售单价")
    private Double price1;

    @ItemProperty(alias = "总药物金额")
    private BigDecimal drugCost;

    @ItemProperty(alias = "备注信息")
    private String memo;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "药物使用频率代码")
    @Dictionary(id = "eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias = "药物使用途径代码")
    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias = "药物使用天数")
    private Integer useDays;

    @ItemProperty(alias = "剂量单位")
    private String useDoseUnit;

    @ItemProperty(alias = "平台商品名")
    private String platformSaleName;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    public CommonRecipeDrugDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDrugStatus() {
        return drugStatus;
    }

    public void setDrugStatus(Integer drugStatus) {
        this.drugStatus = drugStatus;
    }

    public Integer getCommonRecipeId() {
        return commonRecipeId;
    }

    public void setCommonRecipeId(Integer commonRecipeId) {
        this.commonRecipeId = commonRecipeId;
    }

    public Integer getDrugId() {
        return drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getDrugUnit() {
        return drugUnit;
    }

    public void setDrugUnit(String drugUnit) {
        this.drugUnit = drugUnit;
    }

    public String getDrugSpec() {
        return drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    public Double getUseTotalDose() {
        return useTotalDose;
    }

    public void setUseTotalDose(Double useTotalDose) {
        this.useTotalDose = useTotalDose;
    }

    public Double getUseDose() {
        return useDose;
    }

    public void setUseDose(Double useDose) {
        this.useDose = useDose;
    }

    public Double getDefaultUseDose() {
        return defaultUseDose;
    }

    public void setDefaultUseDose(Double defaultUseDose) {
        this.defaultUseDose = defaultUseDose;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public Double getPrice1() {
        return price1;
    }

    public void setPrice1(Double price1) {
        this.price1 = price1;
    }

    public BigDecimal getDrugCost() {
        return drugCost;
    }

    public void setDrugCost(BigDecimal drugCost) {
        this.drugCost = drugCost;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Date getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public String getUsingRate() {
        return usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    public String getUsePathways() {
        return usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    public Integer getUseDays() {
        return useDays;
    }

    public void setUseDays(Integer useDays) {
        this.useDays = useDays;
    }

    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    public String getSaleName() {
        return saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    public String getPlatformSaleName() {
        return platformSaleName;
    }

    public void setPlatformSaleName(String platformSaleName) {
        this.platformSaleName = platformSaleName;
    }

    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }
}
