package com.ngari.recipe.entity;


import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 医疗机构用药目录
 * @author yuyun
 */
@Entity
@Schema
@Table(name = "base_organdruglist")
@Access(AccessType.PROPERTY)
public class OrganDrugList implements java.io.Serializable {
    private static final long serialVersionUID = -2026791423853766129L;

    @ItemProperty(alias = "机构药品序号(自增主键)")
    private Integer organDrugId;

    @ItemProperty(alias = "医疗机构代码(organ表自增主键)")
    private Integer organId;

    @ItemProperty(alias = "平台药品编码(druglist表自增主键)")
    private Integer drugId;

    @ItemProperty(alias = "机构药品唯一索引")
    private String organDrugCode;

    @ItemProperty(alias = "机构药品编码")
    private String drugItemCode;

    @ItemProperty(alias = "通用名/药品名")
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

    @ItemProperty(alias = "出售单位HIS编码")
    private String unitHisCode;

    @ItemProperty(alias = "注册规格单位剂量")
    private Double useDose;

    @ItemProperty(alias = "注册规格默认每次剂量")
    private Double recommendedUseDose;

    @ItemProperty(alias = "注册规格单位")
    private String useDoseUnit;

    @ItemProperty(alias = "注册规格单位HIS编码")
    private String useDoseUnitHisCode;

    @ItemProperty(alias = "开方单位剂量")
    private Double smallestUnitUseDose;

    @ItemProperty(alias = "开方单位默认每次剂量")
    private Double defaultSmallestUnitUseDose;

    @ItemProperty(alias = "通用开方单位")
    private String useDoseSmallestUnit;

    @ItemProperty(alias = "通用开方单位HIS编码")
    private String useDoseSmallestUnitHisCode;

    @ItemProperty(alias = "使用频率平台")
    @Dictionary(id = "eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias = "用药途径平台")
    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias = "使用频率主键id(basic.recipe_usingrate主键)")
    @Dictionary(id = "eh.cdr.dictionary.NewUsingRate")
    private String usingRateId;

    @ItemProperty(alias = "用药途径id(basic.recipe_use_pathways主键)")
    @Dictionary(id = "eh.cdr.dictionary.NewUsePathways")
    private String usePathwaysId;

    @ItemProperty(alias = "生产厂家")
    private String producer;

    @ItemProperty(alias = "搜索关键字，一般包含通用名，商品名及医院自定义值")
    private String searchKey;

    @ItemProperty(alias = "价格")
    private BigDecimal salePrice;

    @ItemProperty(alias = "状态0禁用 1启用")
    @Dictionary(id = "eh.base.dictionary.OrganDrugStatus")
    private Integer status;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "药品产地编码")
    private String producerCode;

    @ItemProperty(alias = "外带药标志 1:是，0否")
    private Integer takeMedicine;

    @ItemProperty(alias = "院内检索关键字")
    private String retrievalCode;

    @ItemProperty(alias = "医院药房名称(维护后数据为空)")
    private String pharmacyName;

    @ItemProperty(alias = "监管平台药品编码")
    private String regulationDrugCode;

    @ItemProperty(alias = "剂型(中文)")
    private String drugForm;

    @ItemProperty(alias = "是否基药：0非基药，1是基药")
    private Integer baseDrug;

    @ItemProperty(alias = "国药准字")
    private String licenseNumber;

    @ItemProperty(alias = "包装材料")
    private String packingMaterials;

    @ItemProperty(alias = "地方医保代码")
    private String medicalDrugCode;
    @ItemProperty(alias = "his剂型代码")
    private String drugFormCode;
    @ItemProperty(alias = "his剂型名称")
    private String hisDrugForm;
    @ItemProperty(alias = "医保剂型编码")
    private String medicalDrugFormCode;

    @ItemProperty(alias = "禁用原因")
    private String disableReason;

    @ItemProperty(alias = "开方药房，主键(一个药品多个药房：325,324)")
    private String pharmacy;

    @ItemProperty(alias = "药品嘱托(中文，对应机构字典表-药品医嘱)")
    private String drugEntrust;

    @ItemProperty(alias = "医保控制：0否，1是")
    private Boolean medicalInsuranceControl;

    @ItemProperty(alias = "适应症说明")
    private String indicationsDeclare;

    @ItemProperty(alias = "是否支持下载处方笺 0   否，1  是   默认1")
    private Boolean supportDownloadPrescriptionPad;

    @ItemProperty(alias = "出售流转药企,主键ids(drugsenterprise) 多选： 1,2 ")
    private String drugsEnterpriseIds;


    @ItemProperty(alias="药品适用业务   历史数据默认 1    1-药品处方 2-诊疗处方 多选： 1,2   eh.base.dictionary.ApplyBusiness ")
    private String applyBusiness;

    @ItemProperty(alias = "药品单复方  0单复方可报  1单方不可报， 复方可报 2 单复方均不可报 ")
    @Dictionary(id = "eh.cdr.dictionary.UnilateralCompound")
    private Integer unilateralCompound;
    @ItemProperty(alias = "是否靶向药  0否  1是 ")
    private Integer targetedDrugType;

    @ItemProperty(alias = "出售单位的销售倍数")
    private Integer smallestSaleMultiple;

    @ItemProperty(alias = "不可在线开具:开关，默认关闭(0),开启（1）")
    private Integer unavailable;

    @ItemProperty(alias = "是否抗肿瘤药物  0否  1是 ")
    private Integer antiTumorDrugFlag;

    @ItemProperty(alias = "抗肿瘤药物等级  1普通级 2限制级 ")
    private Integer antiTumorDrugLevel;

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

    @ItemProperty(alias = "国家医保代码")
    private String nationalMedicalCode;

    @ItemProperty(alias = "是否皮试药品 0 否 1 是")
    private Integer skinTestDrugFlag;

    @ItemProperty(alias = "是否国家标准药品 0 否 1 是")
    private Integer nationalStandardDrugFlag;

    @ItemProperty(alias = "his药品分类名称")
    private String hisDrugClassName;

    @ItemProperty(alias = "his药品分类编码")
    private String hisDrugClassCode;

    @ItemProperty(alias = "中药国标编码")
    private String tcmNationalStandardCode;

    @ItemProperty(alias = "开药数量限制")
    private Integer maximum;

    @ItemProperty(alias = "医保类别")
    private String medicalInsuranceCategory;
    
    @Column(name = "maximum")
    public Integer getMaximum() {
        return maximum;
    }

    public void setMaximum(Integer maximum) {
        this.maximum = maximum;
    }
//    @ItemProperty(alias = "是否冷链运输 0 否 1 是")
//    private Integer coldChainTransportationFlag;
//
//    @Column(name = "cold_chain_transportation_flag")
//    public Integer getColdChainTransportationFlag() {
//        return coldChainTransportationFlag;
//    }
//
//    public void setColdChainTransportationFlag(Integer coldChainTransportationFlag) {
//        this.coldChainTransportationFlag = coldChainTransportationFlag;
//    }

    @Column(name = "tcm_national_standard_code")
    public String getTcmNationalStandardCode() {
        return tcmNationalStandardCode;
    }

    public void setTcmNationalStandardCode(String tcmNationalStandardCode) {
        this.tcmNationalStandardCode = tcmNationalStandardCode;
    }

    @Column(name = "skin_test_drug_flag")
    public Integer getSkinTestDrugFlag() {
        return skinTestDrugFlag;
    }

    public void setSkinTestDrugFlag(Integer skinTestDrugFlag) {
        this.skinTestDrugFlag = skinTestDrugFlag;
    }

    @Column(name = "his_drug_class_name")
    public String getHisDrugClassName() {
        return hisDrugClassName;
    }

    public void setHisDrugClassName(String hisDrugClassName) {
        this.hisDrugClassName = hisDrugClassName;
    }

    @Column(name = "his_drug_class_code")
    public String getHisDrugClassCode() {
        return hisDrugClassCode;
    }

    public void setHisDrugClassCode(String hisDrugClassCode) {
        this.hisDrugClassCode = hisDrugClassCode;
    }

    @Column(name = "national_standard_drug_flag")
    public Integer getNationalStandardDrugFlag() {
        return nationalStandardDrugFlag;
    }

    public void setNationalStandardDrugFlag(Integer nationalStandardDrugFlag) {
        this.nationalStandardDrugFlag = nationalStandardDrugFlag;
    }

    @Column(name = "national_medical_code")
    public String getNationalMedicalCode() {
        return nationalMedicalCode;
    }

    public void setNationalMedicalCode(String nationalMedicalCode) {
        this.nationalMedicalCode = nationalMedicalCode;
    }

    @Column(name = "his_drug_form")
    public String getHisDrugForm() {
        return hisDrugForm;
    }

    public void setHisDrugForm(String hisDrugForm) {
        this.hisDrugForm = hisDrugForm;
    }

    @Column(name = "antibiotics_drug_level")
    public Integer getAntibioticsDrugLevel() {
        return antibioticsDrugLevel;
    }

    public void setAntibioticsDrugLevel(Integer antibioticsDrugLevel) {
        this.antibioticsDrugLevel = antibioticsDrugLevel;
    }

    @Column(name = "standard_code")
    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
    }

    @Column(name = "psychotropic_drug_flag")
    public Integer getPsychotropicDrugFlag() {
        return psychotropicDrugFlag;
    }

    public void setPsychotropicDrugFlag(Integer psychotropicDrugFlag) {
        this.psychotropicDrugFlag = psychotropicDrugFlag;
    }

    @Column(name = "narcotic_drug_flag")
    public Integer getNarcoticDrugFlag() {
        return narcoticDrugFlag;
    }

    public void setNarcoticDrugFlag(Integer narcoticDrugFlag) {
        this.narcoticDrugFlag = narcoticDrugFlag;
    }

    @Column(name = "toxic_drug_flag")
    public Integer getToxicDrugFlag() {
        return toxicDrugFlag;
    }

    public void setToxicDrugFlag(Integer toxicDrugFlag) {
        this.toxicDrugFlag = toxicDrugFlag;
    }

    @Column(name = "radio_activity_drug_flag")
    public Integer getRadioActivityDrugFlag() {
        return radioActivityDrugFlag;
    }

    public void setRadioActivityDrugFlag(Integer radioActivityDrugFlag) {
        this.radioActivityDrugFlag = radioActivityDrugFlag;
    }

    @Column(name = "special_use_antibiotic_drug_flag")
    public Integer getSpecialUseAntibioticDrugFlag() {
        return specialUseAntibioticDrugFlag;
    }

    public void setSpecialUseAntibioticDrugFlag(Integer specialUseAntibioticDrugFlag) {
        this.specialUseAntibioticDrugFlag = specialUseAntibioticDrugFlag;
    }

    @Column(name = "anti_tumor_drug_flag")
    public Integer getAntiTumorDrugFlag() {
        return antiTumorDrugFlag;
    }

    public void setAntiTumorDrugFlag(Integer antiTumorDrugFlag) {
        this.antiTumorDrugFlag = antiTumorDrugFlag;
    }

    @Column(name = "anti_tumor_drug_level")
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

    @Column(name = "smallest_sale_multiple")
    public Integer getSmallestSaleMultiple() {
        return smallestSaleMultiple;
    }

    public void setSmallestSaleMultiple(Integer smallestSaleMultiple) {
        this.smallestSaleMultiple = smallestSaleMultiple;
    }

    @Column(name = "targeted_drug_type")
    public Integer getTargetedDrugType() {
        return targetedDrugType;
    }

    public void setTargetedDrugType(Integer targetedDrugType) {
        this.targetedDrugType = targetedDrugType;
    }

    public OrganDrugList() {
    }

    public OrganDrugList(Integer organDrugId) {
        this.organDrugId = organDrugId;
    }

    public OrganDrugList(Integer organDrugId, Integer organId,
                         Integer drugId, String organDrugCode, Integer status) {
        this.organDrugId = organDrugId;
        this.organId = organId;
        this.drugId = drugId;
        this.organDrugCode = organDrugCode;
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

    @Column(name = "salePrice", precision = 10)
    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    @Column(name = "ProducerCode", length = 30)
    public String getProducerCode() {
        return producerCode;
    }

    public void setProducerCode(String producerCode) {
        this.producerCode = producerCode;
    }

    @Column(name = "TakeMedicine")
    public Integer getTakeMedicine() {
        return takeMedicine;
    }

    public void setTakeMedicine(Integer takeMedicine) {
        this.takeMedicine = takeMedicine;
    }

    @Column(name = "drugName")
    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    @Column(name = "saleName")
    public String getSaleName() {
        return saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    @Column(name = "chemicalName")
    public String getChemicalName() {
        return chemicalName;
    }

    public void setChemicalName(String chemicalName) {
        this.chemicalName = chemicalName;
    }

    @Column(name = "drugSpec")
    public String getDrugSpec() {
        return drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    @Column(name = "pack")
    public Integer getPack() {
        return pack;
    }

    public void setPack(Integer pack) {
        this.pack = pack;
    }

    @Column(name = "unit")
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Column(name = "useDose")
    public Double getUseDose() {
        return useDose;
    }

    public void setUseDose(Double useDose) {
        this.useDose = useDose;
    }

    @Column(name = "recommendedUseDose")
    public Double getRecommendedUseDose() {
        return recommendedUseDose;
    }

    public void setRecommendedUseDose(Double recommendedUseDose) {
        this.recommendedUseDose = recommendedUseDose;
    }

    @Column(name = "useDoseUnit")
    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    @Column(name = "usingRate")
    public String getUsingRate() {
        return usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    @Column(name = "usePathways")
    public String getUsePathways() {
        return usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    @Column(name = "producer")
    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    @Column(name = "searchKey")
    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    @Column(name = "retrievalCode")
    public String getRetrievalCode() {
        return retrievalCode;
    }

    public void setRetrievalCode(String retrievalCode) {
        this.retrievalCode = retrievalCode;
    }

    @Column(name = "pharmacyName")
    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    @Column(name = "regulationDrugCode")
    public String getRegulationDrugCode() {
        return regulationDrugCode;
    }

    public void setRegulationDrugCode(String regulationDrugCode) {
        this.regulationDrugCode = regulationDrugCode;
    }

    @Column(name = "drugForm")
    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }

    @Column(name = "baseDrug")
    public Integer getBaseDrug() {
        return baseDrug;
    }

    public void setBaseDrug(Integer baseDrug) {
        this.baseDrug = baseDrug;
    }

    @Column(name = "licenseNumber")
    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    @Column(name = "medicalDrugCode")
    public String getMedicalDrugCode() {
        return medicalDrugCode;
    }

    public void setMedicalDrugCode(String medicalDrugCode) {
        this.medicalDrugCode = medicalDrugCode;
    }

    @Column(name = "drugFormCode")
    public String getDrugFormCode() {
        return drugFormCode;
    }

    public void setDrugFormCode(String drugFormCode) {
        this.drugFormCode = drugFormCode;
    }

    @Column(name = "MedicalDrugFormCode")
    public String getMedicalDrugFormCode() {
        return medicalDrugFormCode;
    }

    public void setMedicalDrugFormCode(String medicalDrugFormCode) {
        this.medicalDrugFormCode = medicalDrugFormCode;
    }

    @Column(name = "packingMaterials")
    public String getPackingMaterials() {
        return packingMaterials;
    }

    public void setPackingMaterials(String packingMaterials) {
        this.packingMaterials = packingMaterials;
    }

    @Column(name = "disableReason")
    public String getDisableReason() {
        return disableReason;
    }

    public void setDisableReason(String disableReason) {
        this.disableReason = disableReason;
    }

    @Column(name = "smallestUnitUseDose")
    public Double getSmallestUnitUseDose() {
        return smallestUnitUseDose;
    }

    public void setSmallestUnitUseDose(Double smallestUnitUseDose) {
        this.smallestUnitUseDose = smallestUnitUseDose;
    }

    @Column(name = "defaultSmallestUnitUseDose")
    public Double getDefaultSmallestUnitUseDose() {
        return defaultSmallestUnitUseDose;
    }

    public void setDefaultSmallestUnitUseDose(Double defaultSmallestUnitUseDose) {
        this.defaultSmallestUnitUseDose = defaultSmallestUnitUseDose;
    }

    @Column(name = "useDoseSmallestUnit")
    public String getUseDoseSmallestUnit() {
        return useDoseSmallestUnit;
    }

    public void setUseDoseSmallestUnit(String useDoseSmallestUnit) {
        this.useDoseSmallestUnit = useDoseSmallestUnit;
    }

    @Column(name = "usingRateId")
    public String getUsingRateId() {
        return usingRateId;
    }

    public void setUsingRateId(String usingRateId) {
        this.usingRateId = usingRateId;
    }

    @Column(name = "usePathwaysId")
    public String getUsePathwaysId() {
        return usePathwaysId;
    }

    public void setUsePathwaysId(String usePathwaysId) {
        this.usePathwaysId = usePathwaysId;
    }

    @Column(name = "pharmacy")
    public String getPharmacy() {
        return pharmacy;
    }

    public void setPharmacy(String pharmacy) {
        this.pharmacy = pharmacy;
    }

    @Column(name = "drugEntrust")
    public String getDrugEntrust() {
        return drugEntrust;
    }

    public void setDrugEntrust(String drugEntrust) {
        this.drugEntrust = drugEntrust;
    }

    @Column(name = "medicalInsuranceControl")
    public Boolean getMedicalInsuranceControl() {
        return medicalInsuranceControl;
    }

    public void setMedicalInsuranceControl(Boolean medicalInsuranceControl) {
        this.medicalInsuranceControl = medicalInsuranceControl;
    }

    @Column(name = "indicationsDeclare")
    public String getIndicationsDeclare() {
        return indicationsDeclare;
    }

    public void setIndicationsDeclare(String indicationsDeclare) {
        this.indicationsDeclare = indicationsDeclare;
    }

    @Column(name = "supportDownloadPrescriptionPad")
    public Boolean getSupportDownloadPrescriptionPad() {
        return supportDownloadPrescriptionPad;
    }

    public void setSupportDownloadPrescriptionPad(Boolean supportDownloadPrescriptionPad) {
        this.supportDownloadPrescriptionPad = supportDownloadPrescriptionPad;
    }

    @Column(name = "drugsEnterpriseIds")
    public String getDrugsEnterpriseIds() {
        return drugsEnterpriseIds;
    }

    public void setDrugsEnterpriseIds(String drugsEnterpriseIds) {
        this.drugsEnterpriseIds = drugsEnterpriseIds;
    }

    @Column(name = "apply_Business")
    public String getApplyBusiness() {
        return applyBusiness;
    }

    public void setApplyBusiness(String applyBusiness) {
        this.applyBusiness = applyBusiness;
    }

    @Column(name = "unilateral_compound")
    public Integer getUnilateralCompound() {
        return unilateralCompound;
    }

    public void setUnilateralCompound(Integer unilateralCompound) {
        this.unilateralCompound = unilateralCompound;
    }

    @Column(name = "drug_item_code")
    public String getDrugItemCode() {
        return drugItemCode;
    }

    @Column(name = "unit_his_code")
    public String getUnitHisCode() {
        return unitHisCode;
    }

    public void setUnitHisCode(String unitHisCode) {
        this.unitHisCode = unitHisCode;
    }

    @Column(name = "use_dose_unit_his_code")
    public String getUseDoseUnitHisCode() {
        return useDoseUnitHisCode;
    }

    public void setUseDoseUnitHisCode(String useDoseUnitHisCode) {
        this.useDoseUnitHisCode = useDoseUnitHisCode;
    }

    @Column(name = "use_dose_smallest_unit_his_code")
    public String getUseDoseSmallestUnitHisCode() {
        return useDoseSmallestUnitHisCode;
    }

    public void setUseDoseSmallestUnitHisCode(String useDoseSmallestUnitHisCode) {
        this.useDoseSmallestUnitHisCode = useDoseSmallestUnitHisCode;
    }

    public void setDrugItemCode(String drugItemCode) {
        this.drugItemCode = drugItemCode;
    }

    @Column(name = "medical_insurance_category")
    public String getMedicalInsuranceCategory() {
        return medicalInsuranceCategory;
    }

    public void setMedicalInsuranceCategory(String medicalInsuranceCategory) {
        this.medicalInsuranceCategory = medicalInsuranceCategory;
    }
}