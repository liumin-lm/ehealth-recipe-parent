package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

public class RecipeDetailBean implements java.io.Serializable {

    private static final long serialVersionUID = -5228478904040591198L;

    @ItemProperty(alias="处方明细序号")
    private Integer recipeDetailId;

    @ItemProperty(alias="处方序号")
    private Integer recipeId;

    @ItemProperty(alias="药品组号")
    private String drugGroup;

    @ItemProperty(alias="药品序号")
    private Integer drugId;

    @ItemProperty(alias="机构药品编号")
    private String organDrugCode;

    @ItemProperty(alias="药物名称")
    private String drugName;

    @ItemProperty(alias="药物规格")
    private String drugSpec;

    @ItemProperty(alias = "药品包装数量")
    private Integer pack;

    @ItemProperty(alias="药物单位")
    private String drugUnit;

    @ItemProperty(alias="药物使用次剂量")
    private Double useDose;

    @ItemProperty(alias="默认每次剂量")
    private Double defaultUseDose;

    @ItemProperty(alias="药物使用规格单位")
    private String useDoseUnit;

    @ItemProperty(alias="药物剂量单位")
    private String dosageUnit;

    @ItemProperty(alias="药物使用频率代码")
    @Dictionary(id="eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias="药物使用途径代码")
    @Dictionary(id="eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias="药物使用总数量")
    private Double useTotalDose;

    @ItemProperty(alias="药物发放数量")
    private Double sendNumber;

    @ItemProperty(alias="药物使用天数")
    private Integer useDays;

    @ItemProperty(alias="药物金额")
    private BigDecimal drugCost;

    @ItemProperty(alias="备注信息")
    private String memo;

    @ItemProperty(alias="药品效期")
    private Date validDate;

    @ItemProperty(alias="药品批号")
    private String drugBatch;

    @ItemProperty(alias="创建时间")
    private Date createDt;

    @ItemProperty(alias="最后修改时间")
    private Date lastModify;

    @ItemProperty(alias="销售价格")
    private BigDecimal salePrice;

    @ItemProperty(alias="药品编码")
    private String drugCode;

    @ItemProperty(alias="无税单价")
    private BigDecimal price;

    @ItemProperty(alias="税率")
    private Double rate;

    @ItemProperty(alias="含税单价")
    private BigDecimal ratePrice;

    @ItemProperty(alias="无税总额")
    private BigDecimal totalPrice;

    @ItemProperty(alias="税额")
    private BigDecimal tax;

    @ItemProperty(alias="含税总额")
    private BigDecimal totalRatePrice;

    @ItemProperty(alias="医院系统医嘱号")
    private String orderNo;

    @ItemProperty(alias="取药窗口")
    private String pharmNo;

    @ItemProperty(alias = "是否启用")
    private Integer status;

    @ItemProperty(alias="药企发票编号")
    private String invoiceNo;

    @ItemProperty(alias="药企开票日期")
    private Date invoiceDate;

    @ItemProperty(alias="医院给患者发票编号")
    private String patientInvoiceNo;

    @ItemProperty(alias="医院给患者开票日期")
    private Date patientInvoiceDate;

    public RecipeDetailBean() {
    }

    public RecipeDetailBean(Integer recipeId, Integer drugId, String drugName,
                        String drugSpec, String drugUnit, Double sendNumber, BigDecimal drugCost) {
        this.recipeId = recipeId;
        this.drugId = drugId;
        this.drugName = drugName;
        this.drugSpec = drugSpec;
        this.drugUnit = drugUnit;
        this.sendNumber = sendNumber;
        this.drugCost = drugCost;
    }

    public RecipeDetailBean(Integer recipeId, String drugGroup, Integer drugId,
                        String organDrugCode, String drugName, String drugSpec,
                        String drugUnit, Double useDose, String useDoseUnit,
                        String usingRate, String usePathways, Double useTotalDose, Double sendNumber,
                        Integer useDays, BigDecimal salePrice, BigDecimal drugCost, String memo,
                        Timestamp createDt, Timestamp lastModify, Timestamp validDate,
                        String drugBatch, String drugCode, BigDecimal price, Double rate, BigDecimal ratePrice) {
        this.recipeId = recipeId;
        this.drugGroup = drugGroup;
        this.drugId = drugId;
        this.organDrugCode = organDrugCode;
        this.drugName = drugName;
        this.drugSpec = drugSpec;
        this.drugUnit = drugUnit;
        this.useDose = useDose;
        this.useDoseUnit = useDoseUnit;
        this.usingRate = usingRate;
        this.usePathways = usePathways;
        this.useTotalDose = useTotalDose;
        this.sendNumber = sendNumber;
        this.useDays = useDays;
        this.salePrice = salePrice;
        this.drugCost = drugCost;
        this.memo = memo;
        this.createDt = createDt;
        this.lastModify = lastModify;
        this.validDate = validDate;
        this.drugBatch = drugBatch;
        this.drugCode = drugCode;
        this.price = price;
        this.rate = rate;
        this.ratePrice = ratePrice;
    }

    public Integer getRecipeDetailId() {
        return this.recipeDetailId;
    }

    public void setRecipeDetailId(Integer recipeDetailId) {
        this.recipeDetailId = recipeDetailId;
    }

    public Integer getRecipeId() {
        return this.recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public String getDrugGroup() {
        return this.drugGroup;
    }

    public void setDrugGroup(String drugGroup) {
        this.drugGroup = drugGroup;
    }

    public Integer getDrugId() {
        return this.drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    public String getDrugName() {
        return this.drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getDrugSpec() {
        return this.drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    public Integer getPack() {
        return pack;
    }

    public void setPack(Integer pack) {
        this.pack = pack;
    }

    public String getDrugUnit() {
        return this.drugUnit;
    }

    public void setDrugUnit(String drugUnit) {
        this.drugUnit = drugUnit;
    }

    public Double getUseDose() {
        return this.useDose;
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

    public String getUseDoseUnit() {
        return this.useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    public String getDosageUnit() {
        return dosageUnit;
    }

    public void setDosageUnit(String dosageUnit) {
        this.dosageUnit = dosageUnit;
    }

    public String getUsingRate() {
        return this.usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    public String getUsePathways() {
        return this.usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    public Double getUseTotalDose() {
        return this.useTotalDose;
    }

    public void setUseTotalDose(Double useTotalDose) {
        this.useTotalDose = useTotalDose;
    }

    public Double getSendNumber() {
        return this.sendNumber;
    }

    public void setSendNumber(Double sendNumber) {
        this.sendNumber = sendNumber;
    }

    public Integer getUseDays() {
        return this.useDays;
    }

    public void setUseDays(Integer useDays) {
        this.useDays = useDays;
    }
    public BigDecimal getDrugCost() {
        return drugCost;
    }

    public void setDrugCost(BigDecimal drugCost) {
        this.drugCost = drugCost;
    }

    public String getMemo() {
        return this.memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Date getValidDate() {
        return this.validDate;
    }

    public void setValidDate(Date validDate) {
        this.validDate = validDate;
    }

    public String getDrugBatch() {
        return this.drugBatch;
    }

    public void setDrugBatch(String drugBatch) {
        this.drugBatch = drugBatch;
    }
    public Date getCreateDt() {
        return this.createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
    }

    public Date getLastModify() {
        return this.lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public String getDrugCode() {
        return this.drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Double getRate() {
        return this.rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public BigDecimal getRatePrice() {
        return ratePrice;
    }

    public void setRatePrice(BigDecimal ratePrice) {
        this.ratePrice = ratePrice;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getPharmNo() {
        return pharmNo;
    }

    public void setPharmNo(String pharmNo) {
        this.pharmNo = pharmNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotalRatePrice() {
        return totalRatePrice;
    }

    public void setTotalRatePrice(BigDecimal totalRatePrice) {
        this.totalRatePrice = totalRatePrice;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public Date getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(Date invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getPatientInvoiceNo() {
        return patientInvoiceNo;
    }

    public void setPatientInvoiceNo(String patientInvoiceNo) {
        this.patientInvoiceNo = patientInvoiceNo;
    }

    public Date getPatientInvoiceDate() {
        return patientInvoiceDate;
    }

    public void setPatientInvoiceDate(Date patientInvoiceDate) {
        this.patientInvoiceDate = patientInvoiceDate;
    }
}
