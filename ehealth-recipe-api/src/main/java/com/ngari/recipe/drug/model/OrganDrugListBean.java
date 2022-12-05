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

    @ItemProperty(alias = "机构唯一索引")
    private String organDrugCode;

    @ItemProperty(alias = "机构药品编码")
    private String drugItemCode;

    @ItemProperty(alias = "通用名")
    private String drugName;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias = "化学名")
    private String chemicalName;

    @ItemProperty(alias = "药品规格")
    private String drugSpec;

    @ItemProperty(alias = "出售单位下的包装量")
    private Integer pack;

    @ItemProperty(alias = "出售单位")
    private String unit;

    @ItemProperty(alias = "注册规格剂量")
    private Double useDose;

    @ItemProperty(alias = "注册规格默认每次剂量")
    private Double recommendedUseDose;

    @ItemProperty(alias = "注册规格单位")
    private String useDoseUnit;

    @ItemProperty(alias = "开方单位剂量")
    private Double smallestUnitUseDose;

    @ItemProperty(alias = "开方单位默认每次剂量")
    private Double defaultSmallestUnitUseDose;

    @ItemProperty(alias = "通用开方单位")
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

    @ItemProperty(alias = "地方医保代码")
    private String medicalDrugCode;
    @ItemProperty(alias = "医院剂型编码")
    private String drugFormCode;
    @ItemProperty(alias = "医保剂型编码")
    private String medicalDrugFormCode;

    @ItemProperty(alias = "禁用原因")
    private String disableReason;

    @ItemProperty(alias = "开方药房")
    private String pharmacy;

    @ItemProperty(alias = "药品嘱托")
    private String drugEntrust;

    @ItemProperty(alias = "医保控制：0   否，1  是   默认0")
    private Boolean medicalInsuranceControl;

    @ItemProperty(alias = "适应症 说明")
    private String indicationsDeclare;

    @ItemProperty(alias = "是否支持下载处方笺 0   否，1  是   默认1")
    private Boolean supportDownloadPrescriptionPad;

    @ItemProperty(alias = "出售流转药企ids")
    private String drugsEnterpriseIds;

    @ItemProperty(alias="药品适用业务  eh.base.dictionary.ApplyBusiness ")
    private String applyBusiness;

    @ItemProperty(alias="单复方 ")
    @Dictionary(id = "eh.cdr.dictionary.UnilateralCompound")
    private Integer unilateralCompound;

    @ItemProperty(alias = "是否靶向药  0否  1是 ")
    private Integer targetedDrugType;

    @ItemProperty(alias = "出售单位的销售倍数")
    private Integer smallestSaleMultiple;

    @ItemProperty(alias = "销售策略")
    private String salesStrategy;


    @ItemProperty(alias = "不可在线开具:开关，默认关闭(0),开启（1）")
    private Integer unavailable;

    @ItemProperty(alias = "是否抗肿瘤药物  0否  1是 ")
    private Integer antiTumorDrugFlag;

    @ItemProperty(alias = "抗肿瘤药物等级  1普通级 2限制级 ")
    private Integer antiTumorDrugLevel;

    @ItemProperty(alias = "出售单位HIS编码")
    private String unitHisCode;

    @ItemProperty(alias = "注册规格单位HIS编码")
    private String useDoseUnitHisCode;

    @ItemProperty(alias = "通用开方单位HIS编码")
    private String useDoseSmallestUnitHisCode;

    @ItemProperty(alias = "抗菌素药物等级 0：非抗菌素药物 1：1级 2：2级 3：3级 ")
    private Integer antibioticsDrugLevel;

    @ItemProperty(alias = "药品本位码 ")
    private String standardCode;

    @ItemProperty(alias = "是否精神药物  0否  1是 ")
    private Integer psychotropicDrugFlag;

    @ItemProperty(alias = "是否麻醉药物  0否  1是 ")
    private Integer narcoticDrugFlag;

    @ItemProperty(alias = "是否毒性药物  0否  1是 ")
    private Integer toxicDrugFlag;

    @ItemProperty(alias = "是否放射性药物  0否  1是 ")
    private Integer radioActivityDrugFlag;

    @ItemProperty(alias = "是否特殊使用级抗生素药物  0否  1是 ")
    private Integer specialUseAntibioticDrugFlag;

    @ItemProperty(alias = "his剂型名称")
    private String hisDrugForm;

    @ItemProperty(alias = "国家医保代码")
    private String nationalMedicalCode;

    @ItemProperty(alias = "是否皮试药品 0 否 1 是")
    private Integer skinTestDrugFlag;

    @ItemProperty(alias = "是否国家标准药品 0 否 1 是")
    private Integer nationalStandardDrugFlag;

    @ItemProperty(alias = "his药品分类名称 0 否 1 是")
    private String hisDrugClassName;

    @ItemProperty(alias = "his药品分类编码 0 否 1 是")
    private String hisDrugClassCode;

//    @ItemProperty(alias = "是否冷链运输 0 否 1 是")
//    private Integer coldChainTransportationFlag;

    public Integer getSkinTestDrugFlag() {
        return skinTestDrugFlag;
    }

    public void setSkinTestDrugFlag(Integer skinTestDrugFlag) {
        this.skinTestDrugFlag = skinTestDrugFlag;
    }

    public Integer getNationalStandardDrugFlag() {
        return nationalStandardDrugFlag;
    }

    public void setNationalStandardDrugFlag(Integer nationalStandardDrugFlag) {
        this.nationalStandardDrugFlag = nationalStandardDrugFlag;
    }

    public String getHisDrugClassName() {
        return hisDrugClassName;
    }

    public void setHisDrugClassName(String hisDrugClassName) {
        this.hisDrugClassName = hisDrugClassName;
    }

    public String getHisDrugClassCode() {
        return hisDrugClassCode;
    }

    public void setHisDrugClassCode(String hisDrugClassCode) {
        this.hisDrugClassCode = hisDrugClassCode;
    }

//    public Integer getColdChainTransportationFlag() {
//        return coldChainTransportationFlag;
//    }
//
//    public void setColdChainTransportationFlag(Integer coldChainTransportationFlag) {
//        this.coldChainTransportationFlag = coldChainTransportationFlag;
//    }

    public String getNationalMedicalCode() {
        return nationalMedicalCode;
    }

    public void setNationalMedicalCode(String nationalMedicalCode) {
        this.nationalMedicalCode = nationalMedicalCode;
    }

    public String getHisDrugForm() {
        return hisDrugForm;
    }

    public void setHisDrugForm(String hisDrugForm) {
        this.hisDrugForm = hisDrugForm;
    }

    public Integer getAntibioticsDrugLevel() {
        return antibioticsDrugLevel;
    }

    public void setAntibioticsDrugLevel(Integer antibioticsDrugLevel) {
        this.antibioticsDrugLevel = antibioticsDrugLevel;
    }

    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
    }

    public Integer getPsychotropicDrugFlag() {
        return psychotropicDrugFlag;
    }

    public void setPsychotropicDrugFlag(Integer psychotropicDrugFlag) {
        this.psychotropicDrugFlag = psychotropicDrugFlag;
    }

    public Integer getNarcoticDrugFlag() {
        return narcoticDrugFlag;
    }

    public void setNarcoticDrugFlag(Integer narcoticDrugFlag) {
        this.narcoticDrugFlag = narcoticDrugFlag;
    }

    public Integer getToxicDrugFlag() {
        return toxicDrugFlag;
    }

    public void setToxicDrugFlag(Integer toxicDrugFlag) {
        this.toxicDrugFlag = toxicDrugFlag;
    }

    public Integer getRadioActivityDrugFlag() {
        return radioActivityDrugFlag;
    }

    public void setRadioActivityDrugFlag(Integer radioActivityDrugFlag) {
        this.radioActivityDrugFlag = radioActivityDrugFlag;
    }

    public Integer getSpecialUseAntibioticDrugFlag() {
        return specialUseAntibioticDrugFlag;
    }

    public void setSpecialUseAntibioticDrugFlag(Integer specialUseAntibioticDrugFlag) {
        this.specialUseAntibioticDrugFlag = specialUseAntibioticDrugFlag;
    }

    public String getUnitHisCode() {
        return unitHisCode;
    }

    public void setUnitHisCode(String unitHisCode) {
        this.unitHisCode = unitHisCode;
    }

    public String getUseDoseUnitHisCode() {
        return useDoseUnitHisCode;
    }

    public void setUseDoseUnitHisCode(String useDoseUnitHisCode) {
        this.useDoseUnitHisCode = useDoseUnitHisCode;
    }

    public String getUseDoseSmallestUnitHisCode() {
        return useDoseSmallestUnitHisCode;
    }

    public void setUseDoseSmallestUnitHisCode(String useDoseSmallestUnitHisCode) {
        this.useDoseSmallestUnitHisCode = useDoseSmallestUnitHisCode;
    }

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

    public Integer getUnavailable() {
        return unavailable;
    }

    public void setUnavailable(Integer unavailable) {
        this.unavailable = unavailable;
    }

    public Integer getSmallestSaleMultiple() {
        return smallestSaleMultiple;
    }

    public void setSmallestSaleMultiple(Integer smallestSaleMultiple) {
        this.smallestSaleMultiple = smallestSaleMultiple;
    }

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

    public String getSalesStrategy() {
        return salesStrategy;
    }

    public void setSalesStrategy(String salesStrategy) {
        this.salesStrategy = salesStrategy;
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

    public String getChemicalName() {
        return chemicalName;
    }

    public void setChemicalName(String chemicalName) {
        this.chemicalName = chemicalName;
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

    public Boolean getSupportDownloadPrescriptionPad() {
        return supportDownloadPrescriptionPad;
    }

    public void setSupportDownloadPrescriptionPad(Boolean supportDownloadPrescriptionPad) {
        this.supportDownloadPrescriptionPad = supportDownloadPrescriptionPad;
    }

    public String getDrugsEnterpriseIds() {
        return drugsEnterpriseIds;
    }

    public void setDrugsEnterpriseIds(String drugsEnterpriseIds) {
        this.drugsEnterpriseIds = drugsEnterpriseIds;
    }

    public String getApplyBusiness() {
        return applyBusiness;
    }

    public void setApplyBusiness(String applyBusiness) {
        this.applyBusiness = applyBusiness;
    }

    public Integer getUnilateralCompound() {
        return unilateralCompound;
    }

    public void setUnilateralCompound(Integer unilateralCompound) {
        this.unilateralCompound = unilateralCompound;
    }

    public String getDrugItemCode() {
        return drugItemCode;
    }

    public void setDrugItemCode(String drugItemCode) {
        this.drugItemCode = drugItemCode;
    }

    public Integer getTargetedDrugType() {
        return targetedDrugType;
    }

    public void setTargetedDrugType(Integer targetedDrugType) {
        this.targetedDrugType = targetedDrugType;
    }


}