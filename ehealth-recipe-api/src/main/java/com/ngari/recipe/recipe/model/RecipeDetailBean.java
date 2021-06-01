package com.ngari.recipe.recipe.model;

import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * @author yuyun
 */
@Schema
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

    @ItemProperty(alias = "药品商品名")
    private String saleName;

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

    @ItemProperty(alias="药物使用次剂量--中文标识-适量")
    private String useDoseStr;

    @ItemProperty(alias="药物使用规格单位或者最小单位")
    private String useDoseUnit;

    @ItemProperty(alias="药物剂量单位")
    private String dosageUnit;

    @ItemProperty(alias="药物使用频率代码")
    @Dictionary(id="eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias="药物使用途径代码")
    @Dictionary(id="eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(
            alias = "机构的频次代码"
    )
    private String organUsingRate;
    @ItemProperty(
            alias = "机构的用法代码"
    )
    private String organUsePathways;

    //用药频率说明（来源his）
    @ItemProperty(alias="用药频率说明")
    private String usingRateTextFromHis;

    //用药方式说明（来源his）
    @ItemProperty(alias="用药方式说明")
    private String usePathwaysTextFromHis;

    @ItemProperty(alias="药物使用总数量")
    private Double useTotalDose;

    @ItemProperty(alias="药物发放数量")
    private Double sendNumber;

    @ItemProperty(alias = "药物使用天数")
    private Integer useDays;

    @ItemProperty(alias = "药物金额")
    private BigDecimal drugCost;

    @ItemProperty(alias = "药品嘱托Id")
    private String entrustmentId;

    @ItemProperty(alias = "药品嘱托编码")
    private String drugEntrustCode;

    @ItemProperty(alias = "药品嘱托信息")
    private String memo;

    @ItemProperty(alias = "药品效期")
    private Date validDate;

    @ItemProperty(alias = "药品批号")
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

    @ItemProperty(alias="处方明细单号")
    private String recipedtlno;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    //date 20200225
    //同步机构药品信息新添字段
    @ItemProperty(alias = "生产厂家")
    private String producer;

    //date 20200225
    //同步机构药品信息新添字段
    @ItemProperty(alias = "批准文号")
    private String licenseNumber;

    //date 20200225
    //同步机构药品信息新添字段
    @ItemProperty(alias = "生产厂家代码")
    private String producerCode;

    @ItemProperty(alias="药物使用天数小数型")
    private String useDaysB;
    @ItemProperty(alias = "使用频率id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsingRate")
    private String usingRateId;

    @ItemProperty(alias = "用药途径id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsePathways")
    private String usePathwaysId;
    @ItemProperty(alias = "医生端选择的每次剂量和单位绑定关系")
    private List<UseDoseAndUnitRelationBean> useDoseAndUnitRelation;

    @ItemProperty(alias = "开处方时保存单位剂量【规格单位】|单位【规格单位】|单位剂量【最小单位】|单位【最小单位】,各个字段用|隔开，用来计算患者端显示实际每次剂量")
    private String drugUnitdoseAndUnit;

    @ItemProperty(alias = "实际销售价格")
    private BigDecimal actualSalePrice;

    @ItemProperty(alias = "结算方式 0:药店价格 1:医院价格")
    private Integer settlementMode;

    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;
    @ItemProperty(alias = "药房名称")
    private String pharmacyName;
    @ItemProperty(alias = "药房编码")
    private String pharmacyCode;

    @ItemProperty(alias = "中药禁忌类型(1:超量 2:十八反 3:其它)")
    private Integer tcmContraindicationType;

    @ItemProperty(alias = "中药禁忌原因")
    private String tcmContraindicationCause;

    @ItemProperty(alias = "药企药品编码")
    private String saleDrugCode;

    @ItemProperty(alias = "返回药品状态 0:正常，1已失效，2未完善")
    private Integer validateStatus;

    @ItemProperty(alias = "前端展示的药品拼接名")
    private String drugDisplaySplicedName;

    @ItemProperty(alias = "前端展示的商品拼接名")
    private String drugDisplaySplicedSaleName;

    @ItemProperty(alias = "单个药品医保类型")
    private Integer drugMedicalFlag;

    public String getEntrustmentId() {
        return entrustmentId;
    }

    public void setEntrustmentId(String entrustmentId) {
        this.entrustmentId = entrustmentId;
    }

    public String getSaleDrugCode() {
        return saleDrugCode;
    }

    public void setSaleDrugCode(String saleDrugCode) {
        this.saleDrugCode = saleDrugCode;
    }

    public String getUseDaysB() {
        return useDaysB;
    }

    public void setUseDaysB(String useDaysB) {
        this.useDaysB = useDaysB;
    }

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

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getProducerCode() {
        return producerCode;
    }

    public void setProducerCode(String producerCode) {
        this.producerCode = producerCode;
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

    public Integer getTcmContraindicationType() {
        return tcmContraindicationType;
    }

    public void setTcmContraindicationType(Integer tcmContraindicationType) {
        this.tcmContraindicationType = tcmContraindicationType;
    }

    public String getTcmContraindicationCause() {
        return tcmContraindicationCause;
    }

    public void setTcmContraindicationCause(String tcmContraindicationCause) {
        this.tcmContraindicationCause = tcmContraindicationCause;
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

    public String getRecipedtlno() {
        return recipedtlno;
    }

    public void setRecipedtlno(String recipedtlno) {
        this.recipedtlno = recipedtlno;
    }

    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }

    public String getUseDoseStr() {
        return useDoseStr;
    }

    public void setUseDoseStr(String useDoseStr) {
        this.useDoseStr = useDoseStr;
    }

    public String getUsingRateTextFromHis() {
        return usingRateTextFromHis;
    }

    public void setUsingRateTextFromHis(String usingRateTextFromHis) {
        this.usingRateTextFromHis = usingRateTextFromHis;
    }

    public String getUsePathwaysTextFromHis() {
        return usePathwaysTextFromHis;
    }

    public void setUsePathwaysTextFromHis(String usePathwaysTextFromHis) {
        this.usePathwaysTextFromHis = usePathwaysTextFromHis;
    }

    public String getOrganUsingRate() {
        return organUsingRate;
    }

    public void setOrganUsingRate(String organUsingRate) {
        this.organUsingRate = organUsingRate;
    }

    public String getOrganUsePathways() {
        return organUsePathways;
    }

    public void setOrganUsePathways(String organUsePathways) {
        this.organUsePathways = organUsePathways;
    }

    public List<UseDoseAndUnitRelationBean> getUseDoseAndUnitRelation() {
        return useDoseAndUnitRelation;
    }

    public void setUseDoseAndUnitRelation(List<UseDoseAndUnitRelationBean> useDoseAndUnitRelation) {
        this.useDoseAndUnitRelation = useDoseAndUnitRelation;
    }

    public String getUsingRateId() {
        return usingRateId;
    }

    public void setUsingRateId(String usingRateId) {
        this.usingRateId = usingRateId;
    }

    public String getUsePathwaysId() {
        return usePathwaysId;
    }

    public void setUsePathwaysId(String usePathwaysId) {
        this.usePathwaysId = usePathwaysId;
    }

    public String getDrugUnitdoseAndUnit() {
        return drugUnitdoseAndUnit;
    }

    public void setDrugUnitdoseAndUnit(String drugUnitdoseAndUnit) {
        this.drugUnitdoseAndUnit = drugUnitdoseAndUnit;
    }

    public BigDecimal getActualSalePrice() {
        return actualSalePrice;
    }

    public void setActualSalePrice(BigDecimal actualSalePrice) {
        this.actualSalePrice = actualSalePrice;
    }

    public Integer getSettlementMode() {
        return settlementMode;
    }

    public void setSettlementMode(Integer settlementMode) {
        this.settlementMode = settlementMode;
    }

    public Integer getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(Integer pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    public String getSaleName() {
        return saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    public Integer getValidateStatus() {
        return validateStatus;
    }

    public void setValidateStatus(Integer validateStatus) {
        this.validateStatus = validateStatus;
    }

    public String getPharmacyCode() {
        return pharmacyCode;
    }

    public void setPharmacyCode(String pharmacyCode) {
        this.pharmacyCode = pharmacyCode;
    }

    public String getDrugDisplaySplicedName() {
        return drugDisplaySplicedName;
    }

    public void setDrugDisplaySplicedName(String drugDisplaySplicedName) {
        this.drugDisplaySplicedName = drugDisplaySplicedName;
    }

    public String getDrugDisplaySplicedSaleName() {
        return drugDisplaySplicedSaleName;
    }

    public void setDrugDisplaySplicedSaleName(String drugDisplaySplicedSaleName) {
        this.drugDisplaySplicedSaleName = drugDisplaySplicedSaleName;
    }

    public String getDrugEntrustCode() {
        return drugEntrustCode;
    }

    public void setDrugEntrustCode(String drugEntrustCode) {
        this.drugEntrustCode = drugEntrustCode;
    }

    public Integer getDrugMedicalFlag() {
        return drugMedicalFlag;
    }

    public void setDrugMedicalFlag(Integer drugMedicalFlag) {
        this.drugMedicalFlag = drugMedicalFlag;
    }
}
