package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * created by shiyuping on 2019/2/1
 */
@Entity
@Schema
@Table(name = "base_druglist_matching")
@Access(AccessType.PROPERTY)
public class DrugListMatch implements java.io.Serializable {
    public static final long serialVersionUID = -3983203173007645688L;

    @ItemProperty(alias = "匹配记录自增主键")
    private Integer drugId;

    @ItemProperty(alias = "机构药品唯一索引")
    private String organDrugCode;

    @ItemProperty(alias = "机构药品编码")
    private String drugItemCode;

    @Column(name = "organDrugCode", length = 100)
    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    @ItemProperty(alias = "通用名/药品名")
    private String drugName;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias = "化学名")
    private String chemicalName;

    @ItemProperty(alias = "药品规格")
    private String drugSpec;

    @ItemProperty(alias = "包装数量（转化系数）")
    private Integer pack;

    @ItemProperty(alias = "最小规格包装单位")
    private String unit;

    @ItemProperty(alias = "最小售卖单位/单位HIS编码")
    private String unitHisCode;

    @ItemProperty(alias = "药品类型")
    @Dictionary(id = "eh.base.dictionary.DrugType")
    private Integer drugType;

    @ItemProperty(alias = "单次剂量（规格单位）")
    private Double useDose;

    @ItemProperty(alias = "规格单位")
    private String useDoseUnit;

    @ItemProperty(alias = "规格单位/单位HIS编码")
    private String useDoseUnitHisCode;

    @ItemProperty(alias = "生产厂家")
    private String producer;

    @ItemProperty(alias = "价格")
    private BigDecimal price;

    @ItemProperty(alias = "状态 0未匹配 1已匹配 2已提交 3已标记 4匹配中")
    private Integer status;

    @ItemProperty(alias = "适应症说明 为什么下面还整了一个IndicationsDeclare 当老字段了 后面又整了个新的")
    private String indications;

    @ItemProperty(alias = "拼音码")
    private String pyCode;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "批准文号")
    private String approvalNumber;

    @ItemProperty(alias = "国药准字")
    private String licenseNumber;

    @ItemProperty(alias = "药品本位码")
    private String standardCode;

    @Column(name = "licenseNumber", length = 30)
    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    @Column(name = "standardCode", length = 30)
    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
    }

    @ItemProperty(alias = "匹配的平台通用药品id(drugList表主键)")
    private Integer matchDrugId;

    @ItemProperty(alias = "是否是新增药品")
    private Integer isNew;

    @ItemProperty(alias = "来源机构??")
    private Integer sourceOrgan;

    @ItemProperty(alias = "剂型(中文)")
    private String drugForm;

    @ItemProperty(alias = "包装材料")
    private String packingMaterials;

    @ItemProperty(alias = "是否基药")
    private Integer baseDrug;

    @ItemProperty(alias = "操作人")
    private String operator;

    @ItemProperty(alias = "用药频次(basic.recipe_usingrate.relatedPlatformKey)")
    @Dictionary(id = "eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias = "用药途径(basic.recipe_use_pathways.relatedPlatformKey)")
    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias = "使用频率主键id(basic.recipe_usingrate主键)")
    @Dictionary(id = "eh.cdr.dictionary.NewUsingRate")
    private String usingRateId;

    @ItemProperty(alias = "用药途径id(basic.recipe_use_pathways主键)")
    @Dictionary(id = "eh.cdr.dictionary.NewUsePathways")
    private String usePathwaysId;

    @ItemProperty(alias = "默认单次剂量（规格单位）对应recommendedUseDose")
    private Double defaultUseDose;

    @ItemProperty(alias = "院内搜索关键字")
    private String retrievalCode;

    @ItemProperty(alias = "监管平台药品编码")
    private String regulationDrugCode;

    @ItemProperty(alias = "药品产地编码 对应ProducerCode")
    private String drugManfCode;

    @ItemProperty(alias = "医保药品编码")
    private String medicalDrugCode;

    @ItemProperty(alias = "医保剂型代码")
    private String medicalDrugFormCode;

    @ItemProperty(alias = "HIS剂型代码 DrugFormCode")
    private String hisFormCode;

    @ItemProperty(alias = "平台药品id(drugList表主键)")
    private Integer platformDrugId;

    @ItemProperty(alias = "药房(一个药品多个药房：325,324)")
    private String pharmacy;

    @ItemProperty(alias = "药品嘱托(中文)")
    private String drugEntrust;

    @ItemProperty(alias = "医保控制：0   否，1  是   默认0")
    private Boolean medicalInsuranceControl;

    @ItemProperty(alias = "适应症说明")
    private String IndicationsDeclare;

    @ItemProperty(alias = "单次剂量(最小规格包装单位)")
    private Double smallestUnitUseDose;

    @ItemProperty(alias = "默认单次剂量（规格单位）")
    private Double recommendedUseDose;

    @ItemProperty(alias = "配送药企主键(drugsenterprise)多选： 1,2 ")
    private String drugsEnterpriseIds;


    @ItemProperty(alias = "最小规格包装单位")
    private String useDoseSmallestUnit;

    @ItemProperty(alias = "单位剂量单位（最小单位）/单位his编码")
    private String useDoseSmallestUnitHisCode;

    @ItemProperty(alias = "药品适用业务   历史数据默认 1    1-药品处方 2-诊疗处方 选： 1,2 ")
    private String applyBusiness;


    @ItemProperty(alias = "药品来源  0 批量导入 1 手动同步")
    @Dictionary(id = "eh.cdr.dictionary.DrugListMatchSource")
    private Integer drugSource;

    @ItemProperty(alias = "药品单复方  0  单复方可报  1单方不可报， 复方可报 2 单复方均不可报 ")
    @Dictionary(id = "eh.cdr.dictionary.UnilateralCompound")
    private Integer unilateralCompound;

    @ItemProperty(alias = "默认单次剂量（最小规格包装单位）")
    private Double defaultSmallestUnitUseDose;

    @ItemProperty(alias = "是否靶向药  0否  1是 ")
    private Integer targetedDrugType;

    @ItemProperty(alias = "最小销售倍数")
    private Integer smallestSaleMultiple;

    @ItemProperty(alias = "是否抗肿瘤药物  0否  1是 ")
    private Integer antiTumorDrugFlag;

    @ItemProperty(alias = "抗肿瘤药物等级  1普通级 2限制级 ")
    private Integer antiTumorDrugLevel;

    @ItemProperty(alias = "不可在线开具:开关，默认关闭(0),开启（1）")
    private Integer unavailable;

    @ItemProperty(alias = "抗菌素药物等级 0：非抗菌素药物 1：1级 2：2级 3：3级 ")
    private Integer antibioticsDrugLevel;

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

    @Column(name = "antibiotics_drug_level")
    public Integer getAntibioticsDrugLevel() {
        return antibioticsDrugLevel;
    }

    public void setAntibioticsDrugLevel(Integer antibioticsDrugLevel) {
        this.antibioticsDrugLevel = antibioticsDrugLevel;
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

    public Integer getUnavailable() {
        return unavailable;
    }

    public void setUnavailable(Integer unavailable) {
        this.unavailable = unavailable;
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

    @Column(name = "defaultSmallestUnitUseDose ")
    public Double getDefaultSmallestUnitUseDose() {
        return defaultSmallestUnitUseDose;
    }

    public void setDefaultSmallestUnitUseDose(Double defaultSmallestUnitUseDose) {
        this.defaultSmallestUnitUseDose = defaultSmallestUnitUseDose;
    }

    @Column(name = "retrievalCode ")
    public String getRetrievalCode() {
        return retrievalCode;
    }

    public void setRetrievalCode(String retrievalCode) {
        this.retrievalCode = retrievalCode;
    }

    @Column(name = "usingRate", length = 10)
    public String getUsingRate() {
        return usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    @Column(name = "usePathways", length = 10)
    public String getUsePathways() {
        return usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    @Column(name = "defaultUseDose", precision = 10, scale = 3)
    public Double getDefaultUseDose() {
        return defaultUseDose;
    }

    public void setDefaultUseDose(Double defaultUseDose) {
        this.defaultUseDose = defaultUseDose;
    }

    @Column(name = "operator", length = 20)
    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Column(name = "drugForm", length = 20)
    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }

    @Column(name = "packingMaterials", length = 50)
    public String getPackingMaterials() {
        return packingMaterials;
    }

    public void setPackingMaterials(String packingMaterials) {
        this.packingMaterials = packingMaterials;
    }

    @Column(name = "baseDrug", length = 1)
    public Integer getBaseDrug() {
        return baseDrug;
    }

    public void setBaseDrug(Integer baseDrug) {
        this.baseDrug = baseDrug;
    }

    @Column(name = "matchDrugId", length = 11)
    public Integer getMatchDrugId() {
        return matchDrugId;
    }

    public void setMatchDrugId(Integer matchDrugId) {
        this.matchDrugId = matchDrugId;
    }

    @Column(name = "isNew", length = 1)
    public Integer getIsNew() {
        return isNew;
    }

    public void setIsNew(Integer isNew) {
        this.isNew = isNew;
    }

    @Column(name = "sourceOrgan", length = 11)
    public Integer getSourceOrgan() {
        return sourceOrgan;
    }

    public void setSourceOrgan(Integer sourceOrgan) {
        this.sourceOrgan = sourceOrgan;
    }

    public DrugListMatch() {
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "drugId", unique = true, nullable = false)
    public Integer getDrugId() {
        return this.drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    @Column(name = "drugName", length = 50)
    public String getDrugName() {
        return this.drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    @Column(name = "saleName", length = 50)
    public String getSaleName() {
        return this.saleName;
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

    @Column(name = "drugSpec", length = 50)
    public String getDrugSpec() {
        return this.drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    @Column(name = "unit", length = 6)
    public String getUnit() {
        return this.unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Column(name = "drugType")
    public Integer getDrugType() {
        return this.drugType;
    }

    public void setDrugType(Integer drugType) {
        this.drugType = drugType;
    }

    @Column(name = "useDose", precision = 10, scale = 3)
    public Double getUseDose() {
        return this.useDose;
    }

    public void setUseDose(Double useDose) {
        this.useDose = useDose;
    }

    @Column(name = "useDoseUnit")
    public String getUseDoseUnit() {
        return this.useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }


    @Column(name = "producer", length = 20)
    public String getProducer() {
        return this.producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    @Column(name = "price", precision = 10)
    public BigDecimal getPrice() {
        return this.price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Column(name = "status")
    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "indications", length = 50)
    public String getIndications() {
        return this.indications;
    }

    public void setIndications(String indications) {
        this.indications = indications;
    }

    @Column(name = "pyCode", length = 20)
    public String getPyCode() {
        return this.pyCode;
    }

    public void setPyCode(String pyCode) {
        this.pyCode = pyCode;
    }

    @Column(name = "createDt", length = 19)
    public Date getCreateDt() {
        return this.createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
    }

    @Column(name = "lastModify", length = 19)
    public Date getLastModify() {
        return this.lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    @Column(name = "pack", nullable = false, length = 11)
    public Integer getPack() {
        return pack;
    }

    public void setPack(Integer pack) {
        this.pack = pack;
    }

    @Column(name = "approvalNumber", length = 100)
    public String getApprovalNumber() {
        return approvalNumber;
    }

    public void setApprovalNumber(String approvalNumber) {
        this.approvalNumber = approvalNumber;
    }

    @Column(name = "regulationDrugCode", length = 20)
    public String getRegulationDrugCode() {
        return regulationDrugCode;
    }

    public void setRegulationDrugCode(String regulationDrugCode) {
        this.regulationDrugCode = regulationDrugCode;
    }

    @Column(name = "drugManfCode", length = 30)
    public String getDrugManfCode() {
        return drugManfCode;
    }

    public void setDrugManfCode(String drugManfCode) {
        this.drugManfCode = drugManfCode;
    }

    @Column(name = "medicalDrugCode", length = 100)
    public String getMedicalDrugCode() {
        return medicalDrugCode;
    }

    public void setMedicalDrugCode(String medicalDrugCode) {
        this.medicalDrugCode = medicalDrugCode;
    }

    @Column(name = "medicalDrugFormCode", length = 30)
    public String getMedicalDrugFormCode() {
        return medicalDrugFormCode;
    }

    public void setMedicalDrugFormCode(String medicalDrugFormCode) {
        this.medicalDrugFormCode = medicalDrugFormCode;
    }

    @Column(name = "hisFormCode", length = 30)
    public String getHisFormCode() {
        return hisFormCode;
    }

    public void setHisFormCode(String hisFormCode) {
        this.hisFormCode = hisFormCode;
    }

    @Column(name = "platformDrugId", length = 11)
    public Integer getPlatformDrugId() {
        return platformDrugId;
    }

    public void setPlatformDrugId(Integer platformDrugId) {
        this.platformDrugId = platformDrugId;
    }

    @Column(name = "pharmacy", length = 12)
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
        return IndicationsDeclare;
    }

    public void setIndicationsDeclare(String indicationsDeclare) {
        IndicationsDeclare = indicationsDeclare;
    }

    @Column(name = "smallestUnitUseDose")
    public Double getSmallestUnitUseDose() {
        return smallestUnitUseDose;
    }

    public void setSmallestUnitUseDose(Double smallestUnitUseDose) {
        this.smallestUnitUseDose = smallestUnitUseDose;
    }

    @Column(name = "recommendedUseDose")
    public Double getRecommendedUseDose() {
        return recommendedUseDose;
    }

    public void setRecommendedUseDose(Double recommendedUseDose) {
        this.recommendedUseDose = recommendedUseDose;
    }

    @Column(name = "drugsEnterpriseIds")
    public String getDrugsEnterpriseIds() {
        return drugsEnterpriseIds;
    }

    public void setDrugsEnterpriseIds(String drugsEnterpriseIds) {
        this.drugsEnterpriseIds = drugsEnterpriseIds;
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

    @Column(name = "useDoseSmallestUnit")
    public String getUseDoseSmallestUnit() {
        return useDoseSmallestUnit;
    }

    public void setUseDoseSmallestUnit(String useDoseSmallestUnit) {
        this.useDoseSmallestUnit = useDoseSmallestUnit;
    }

    @Column(name = "drug_source")
    public Integer getDrugSource() {
        return drugSource;
    }

    public void setDrugSource(Integer drugSource) {
        this.drugSource = drugSource;
    }

    @Column(name = "apply_Business")
    public String getApplyBusiness() {
        return applyBusiness;
    }

    public void setApplyBusiness(String applyBusiness) {
        this.applyBusiness = applyBusiness;
    }

    @Column(name = "drug_item_code")
    public String getDrugItemCode() {
        return drugItemCode;
    }

    public void setDrugItemCode(String drugItemCode) {
        this.drugItemCode = drugItemCode;
    }

    @Column(name = "unilateral_compound")
    public Integer getUnilateralCompound() {
        return unilateralCompound;
    }

    public void setUnilateralCompound(Integer unilateralCompound) {
        this.unilateralCompound = unilateralCompound;
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
}
