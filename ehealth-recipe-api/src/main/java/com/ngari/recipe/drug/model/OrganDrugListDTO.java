package com.ngari.recipe.drug.model;


import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 医疗机构用药目录
 *
 * @author yuyun
 */
@Schema
public class OrganDrugListDTO implements java.io.Serializable {

    private static final long serialVersionUID = -2632080863700669392L;

    @ItemProperty(alias = "机构药品序号")
    private Integer organDrugId;

    @ItemProperty(alias = "医疗机构代码")
    private Integer organId;

    @ItemProperty(alias = "药品序号")
    private Integer drugId;

    @ItemProperty(alias = "通用名")
    private String drugName;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias = "机构药品编码")
    private String organDrugCode;

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
    @ItemProperty(alias = "HIS剂型编码")
    private String drugFormCode;
    @ItemProperty(alias = "医保剂型编码")
    private String medicalDrugFormCode;

    @ItemProperty(alias = "禁用原因")
    private String disableReason;

    @ItemProperty(alias = "推荐单次剂量")
    private Double recommendedUseDose;

    @ItemProperty(alias = "转换系数")
    private Integer pack;

    @ItemProperty(alias = "药品包装单位")
    private String unit;

    @ItemProperty(alias = "使用频率")
    @Dictionary(id = "eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias = "用药途径")
    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias = "单次剂量单位")
    private String useDoseUnit;

    @ItemProperty(alias = "生产厂家")
    private String producer;

    @ItemProperty(alias = "药房")
    private String pharmacy;

    @ItemProperty(alias = "药品嘱托")
    private String drugEntrust;

    @ItemProperty(alias = "医保控制：0   否，1  是   默认0")
    private Boolean medicalInsuranceControl;

    @ItemProperty(alias = "适应症 说明")
    private String indicationsDeclare;

    public OrganDrugListDTO() {
    }

    public Integer getOrganDrugId() {
        return organDrugId;
    }

    public void setOrganDrugId(Integer organDrugId) {
        this.organDrugId = organDrugId;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
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

    public String getSaleName() {
        return saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public String getRetrievalCode() {
        return retrievalCode;
    }

    public void setRetrievalCode(String retrievalCode) {
        this.retrievalCode = retrievalCode;
    }

    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    public String getRegulationDrugCode() {
        return regulationDrugCode;
    }

    public void setRegulationDrugCode(String regulationDrugCode) {
        this.regulationDrugCode = regulationDrugCode;
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

    public String getPackingMaterials() {
        return packingMaterials;
    }

    public void setPackingMaterials(String packingMaterials) {
        this.packingMaterials = packingMaterials;
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

    public String getDisableReason() {
        return disableReason;
    }

    public void setDisableReason(String disableReason) {
        this.disableReason = disableReason;
    }

    public Double getRecommendedUseDose() {
        return recommendedUseDose;
    }

    public void setRecommendedUseDose(Double recommendedUseDose) {
        this.recommendedUseDose = recommendedUseDose;
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

    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
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
        indicationsDeclare = indicationsDeclare;
    }
}