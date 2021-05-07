package com.ngari.recipe.drug.model;


import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 医疗机构用药目录
 * @author yinsheng
 */
@Schema
public class OrganDrugListBean implements java.io.Serializable {

    private static final long serialVersionUID = -8703128826623125579L;

    @ItemProperty(alias = "机构药品序号")
    private Integer organDrugId;

    @ItemProperty(alias = "医疗机构代码")
    private Integer organId;

    @ItemProperty(alias = "药品序号")
    private Integer drugId;

    @ItemProperty(alias = "机构药品编码")
    private String organDrugCode;

    @ItemProperty(alias = "通用名")
    private String drugName;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias = "药品规格")
    private String drugSpec;

    @ItemProperty(alias = "转换系数")
    private Integer pack;

    @ItemProperty(alias = "药品包装单位")
    private String unit;

    @ItemProperty(alias = "实际单次剂量")
    private Double useDose;

    @ItemProperty(alias = "推荐单次剂量")
    private Double recommendedUseDose;

    @ItemProperty(alias = "单次剂量单位")
    private String useDoseUnit;

    @ItemProperty(alias = "实际单位剂量（最小单位）")
    private Double smallestUnitUseDose;

    @ItemProperty(alias = "默认单位剂量（最小单位）")
    private Double defaultSmallestUnitUseDose;

    @ItemProperty(alias = "单位剂量单位（最小单位）")
    private String useDoseSmallestUnit;

    @ItemProperty(alias = "使用频率平台")
    @Dictionary(id = "eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias = "用药途径平台")
    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias = "使用频率id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsingRate")
    private String usingRateId;

    @ItemProperty(alias = "用药途径id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsePathways")
    private String usePathwaysId;

    @ItemProperty(alias = "生产厂家")
    private String producer;

    @ItemProperty(alias = "搜索关键字，一般包含通用名，商品名及医院自定义值")
    private String searchKey;

    @ItemProperty(alias = "销售价格")
    private BigDecimal salePrice;

    @ItemProperty(alias = "使用状态")
    @Dictionary(id = "eh.base.dictionary.OrganDrugStatus")
    private Integer status;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后生成时间")
    private Date lastModify;

    @ItemProperty(alias = "生产厂家代码")
    private String producerCode;

    @ItemProperty(alias = "外带药标志 1:外带药")
    private Integer takeMedicine;

    @ItemProperty(alias = "院内检索关键字")
    private String retrievalCode;

    @ItemProperty(alias = "医院药房名字")
    private String pharmacyName;

    @ItemProperty(alias = "监管平台药品编码")
    private String regulationDrugCode;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    @ItemProperty(alias = "是否基药")
    private Integer baseDrug;

    @ItemProperty(alias = "批准文号")
    private String licenseNumber;

    @ItemProperty(alias = "包装材料")
    private String packingMaterials;

    @ItemProperty(alias = "医保药品编码")
    private String medicalDrugCode;
    @ItemProperty(alias = "医院剂型编码")
    private String drugFormCode;
    @ItemProperty(alias = "医保剂型编码")
    private String medicalDrugFormCode;

    @ItemProperty(alias = "禁用原因")
    private String disableReason;

    @ItemProperty(alias = "药房")
    private String pharmacy;

    @ItemProperty(alias = "药品嘱托")
    private String drugEntrust;

    @ItemProperty(alias = "医保控制：0   否，1  是   默认0")
    private Boolean medicalInsuranceControl;

    @ItemProperty(alias = "适应症 说明")
    private String indicationsDeclare;

    public OrganDrugListBean() {
    }

    public OrganDrugListBean(Integer organDrugId) {
        this.organDrugId = organDrugId;
    }

    public OrganDrugListBean(Integer organDrugId, Integer organId,
                             Integer drugId, String organDrugCode, Integer status) {
        this.organDrugId = organDrugId;
        this.organId = organId;
        this.drugId = drugId;
        this.organDrugCode = organDrugCode;
        this.status = status;
    }

    public Integer getOrganDrugId() {
        return this.organDrugId;
    }

    public void setOrganDrugId(Integer organDrugId) {
        this.organDrugId = organDrugId;
    }

    public Integer getOrganId() {
        return this.organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public Integer getDrugId() {
        return this.drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    public String getOrganDrugCode() {
        return this.organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public String getProducerCode() {
        return producerCode;
    }

    public void setProducerCode(String producerCode) {
        this.producerCode = producerCode;
    }

    public Integer getTakeMedicine() {
        return takeMedicine;
    }

    public void setTakeMedicine(Integer takeMedicine) {
        this.takeMedicine = takeMedicine;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getSaleName() {
        return saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    public String getDrugSpec() {
        return drugSpec;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getUseDose() {
        return useDose;
    }

    public void setUseDose(Double useDose) {
        this.useDose = useDose;
    }

    public Double getRecommendedUseDose() {
        return recommendedUseDose;
    }

    public void setRecommendedUseDose(Double recommendedUseDose) {
        this.recommendedUseDose = recommendedUseDose;
    }

    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
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

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public String getRetrievalCode() {
        return retrievalCode;
    }

    public void setRetrievalCode(String retrievalCode) {
        this.retrievalCode = retrievalCode;
    }

    public String getRegulationDrugCode() {
        return regulationDrugCode;
    }

    public void setRegulationDrugCode(String regulationDrugCode) {
        this.regulationDrugCode = regulationDrugCode;
    }

    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }

    public Integer getBaseDrug() {
        return baseDrug;
    }

    public void setBaseDrug(Integer baseDrug) {
        this.baseDrug = baseDrug;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getMedicalDrugCode() {
        return medicalDrugCode;
    }

    public void setMedicalDrugCode(String medicalDrugCode) {
        this.medicalDrugCode = medicalDrugCode;
    }

    public String getDrugFormCode() {
        return drugFormCode;
    }

    public void setDrugFormCode(String drugFormCode) {
        this.drugFormCode = drugFormCode;
    }

    public String getMedicalDrugFormCode() {
        return medicalDrugFormCode;
    }

    public void setMedicalDrugFormCode(String medicalDrugFormCode) {
        this.medicalDrugFormCode = medicalDrugFormCode;
    }

    public String getPackingMaterials() {
        return packingMaterials;
    }

    public void setPackingMaterials(String packingMaterials) {
        this.packingMaterials = packingMaterials;
    }

    public String getDisableReason() {
        return disableReason;
    }

    public void setDisableReason(String disableReason) {
        this.disableReason = disableReason;
    }

    public Double getSmallestUnitUseDose() {
        return smallestUnitUseDose;
    }

    public void setSmallestUnitUseDose(Double smallestUnitUseDose) {
        this.smallestUnitUseDose = smallestUnitUseDose;
    }

    public Double getDefaultSmallestUnitUseDose() {
        return defaultSmallestUnitUseDose;
    }

    public void setDefaultSmallestUnitUseDose(Double defaultSmallestUnitUseDose) {
        this.defaultSmallestUnitUseDose = defaultSmallestUnitUseDose;
    }

    public String getUseDoseSmallestUnit() {
        return useDoseSmallestUnit;
    }

    public void setUseDoseSmallestUnit(String useDoseSmallestUnit) {
        this.useDoseSmallestUnit = useDoseSmallestUnit;
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

    public String getPharmacy() {
        return pharmacy;
    }

    public void setPharmacy(String pharmacy) {
        this.pharmacy = pharmacy;
    }

    public String getDrugEntrust() {
        return drugEntrust;
    }

    public void setDrugEntrust(String drugEntrust) {
        this.drugEntrust = drugEntrust;
    }

    public Boolean getMedicalInsuranceControl() {
        return medicalInsuranceControl;
    }

    public void setMedicalInsuranceControl(Boolean medicalInsuranceControl) {
        this.medicalInsuranceControl = medicalInsuranceControl;
    }

    public String getIndicationsDeclare() {
        return indicationsDeclare;
    }

    public void setIndicationsDeclare(String indicationsDeclare) {
        this.indicationsDeclare = indicationsDeclare;
    }
}