package com.ngari.recipe.common;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;

import java.io.Serializable;
import java.math.BigDecimal;

//JRK
//date 20200227 机构药品更新保存信息对象
public class OrganDrugChangeBean implements Serializable {

    private static final long serialVersionUID = -3785008998719273890L;

    @ItemProperty(alias = "医疗机构代码 organId")
    private Integer organId;

    @ItemProperty(alias = "药品序号 superviseDrugCode")
    private Integer drugId;

    @ItemProperty(alias = "机构药品编码 drugCode")
    private String organDrugCode;

    @ItemProperty(alias = "通用名 drugGenericName")
    private String drugName;

    @ItemProperty(alias = "商品名 drugTradeName")
    private String saleName;

    @ItemProperty(alias = "药品规格 drugModel")
    private String drugSpec;

    @ItemProperty(alias = "转换系数 packSpecs")
    private Integer pack;

    @ItemProperty(alias = "药品包装单位 packUnit")
    private String unit;

    @ItemProperty(alias = "实际单次剂量 unitContent")
    private Double useDose;

    @ItemProperty(alias = "推荐单次剂量 defaultUseDose")
    private Double recommendedUseDose;

    @ItemProperty(alias = "单次剂量单位 DoseUnit")
    private String useDoseUnit;

    @ItemProperty(alias = "使用频率 defaultUsingRate")
    @Dictionary(id = "eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias = "用药途径 defaultUsePathways")
    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias = "生产厂家 producer")
    private String producer;

    @ItemProperty(alias = "销售价格 drugFee")
    private BigDecimal salePrice;

    @ItemProperty(alias = "生产厂家代码 producerId")
    private String producerCode;

    @ItemProperty(alias = "是否基药 basicMedicine 0 否 1 是")
    private Integer baseDrug;

    @ItemProperty(alias = "批准文号 ChinsesMedCode")
    private String licenseNumber;

    @ItemProperty(alias = "医保药品编码 insuDrugCode")
    private String medicalDrugCode;
    @ItemProperty(alias = "医院剂型编码")
    private String drugFormCode;
    @ItemProperty(alias = "his医院剂型名称")
    private String hisDrugForm;
    @ItemProperty(alias = "医保剂型代码")
    private String medicalDrugFormCode;

    public String getMedicalDrugFormCode() {
        return medicalDrugFormCode;
    }

    public void setMedicalDrugFormCode(String medicalDrugFormCode) {
        this.medicalDrugFormCode = medicalDrugFormCode;
    }

    public String getDrugFormCode() {
        return drugFormCode;
    }

    public void setDrugFormCode(String drugFormCode) {
        this.drugFormCode = drugFormCode;
    }

    public String getHisDrugForm() {
        return hisDrugForm;
    }

    public void setHisDrugForm(String hisDrugForm) {
        this.hisDrugForm = hisDrugForm;
    }

    @ItemProperty(alias = "修改的方式 1新增 2修改 3停用  operationCode")
    private Integer operationCode;

    @ItemProperty(alias = "云药房药品代码  cloudPharmDrugCode")
    private String cloudPharmDrugCode;

    @ItemProperty(alias = "药品医保类别  drtt")
    private Integer medicalDrugType;

    @ItemProperty(alias = "1西药 2成药 3草药  drugType")
    private Integer drugType;

    @ItemProperty(alias = "机构名称  organName")
    private String organName;

    @ItemProperty(alias="是否靶向药  0否  1是 ")
    private Integer targetedDrugType;

    @ItemProperty(alias = "最小销售倍数")
    private Integer smallestSaleMultiple;

    @ItemProperty(alias = "是否抗肿瘤药物  0否  1是 ")
    private Integer antiTumorDrugFlag;

    @ItemProperty(alias = "抗肿瘤药物等级  1普通级 2限制级 ")
    private Integer antiTumorDrugLevel;

    public Integer getAntiTumorDrugFlag() {
        return antiTumorDrugFlag;
    }

    public void setAntiTumorDrugFlag(Integer antiTumorDrugFlag) {
        this.antiTumorDrugFlag = antiTumorDrugFlag;
    }

    public Integer getAntiTumorDrugLevel() {
        return antiTumorDrugLevel;
    }

    public void setAntiTumorDrugLevel(Integer antiTumorDrugLevel) {
        this.antiTumorDrugLevel = antiTumorDrugLevel;
    }

    public Integer getTargetedDrugType() {
        return targetedDrugType;
    }

    public void setTargetedDrugType(Integer targetedDrugType) {
        this.targetedDrugType = targetedDrugType;
    }

    public Integer getSmallestSaleMultiple() {
        return smallestSaleMultiple;
    }

    public void setSmallestSaleMultiple(Integer smallestSaleMultiple) {
        this.smallestSaleMultiple = smallestSaleMultiple;
    }

    public Integer getMedicalDrugType() {
        return medicalDrugType;
    }

    public void setMedicalDrugType(Integer medicalDrugType) {
        this.medicalDrugType = medicalDrugType;
    }

    public Integer getDrugType() {
        return drugType;
    }

    public void setDrugType(Integer drugType) {
        this.drugType = drugType;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getCloudPharmDrugCode() {
        return cloudPharmDrugCode;
    }

    public void setCloudPharmDrugCode(String cloudPharmDrugCode) {
        this.cloudPharmDrugCode = cloudPharmDrugCode;
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

    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
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

    public Integer getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(Integer operationCode) {
        this.operationCode = operationCode;
    }

}