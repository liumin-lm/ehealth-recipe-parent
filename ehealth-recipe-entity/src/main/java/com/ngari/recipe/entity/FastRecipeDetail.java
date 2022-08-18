package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @Description
 * @Author yzl
 * @Date 2022-08-16
 */
@Entity
@Schema
@Table(name = "cdr_fast_recipe_detail")
@Access(AccessType.PROPERTY)
public class FastRecipeDetail {

    @ItemProperty(alias = "主键")
    private Integer id;

    @ItemProperty(alias = "药方Id")
    private Integer fastRecipeId;

    @ItemProperty(alias = "药品商品名")
    private String saleName;

    @ItemProperty(alias = "药品序号")
    private Integer drugId;

    @ItemProperty(alias = "机构唯一索引")
    private String organDrugCode;

    @ItemProperty(alias = "机构药品编号")
    private String drugItemCode;

    @ItemProperty(alias = "药物名称")
    private String drugName;

    @ItemProperty(alias = "药物规格")
    private String drugSpec;

    @ItemProperty(alias = "药品包装数量")
    private Integer pack;

    @ItemProperty(alias = "药物单位")
    private String drugUnit;

    @ItemProperty(alias = "药物使用次剂量")
    private Double useDose;

    @ItemProperty(alias = "默认每次剂量")
    private Double defaultUseDose;

    @ItemProperty(alias = "药物使用次剂量--中文标识-适量")
    private String useDoseStr;

    @ItemProperty(alias = "药物使用规格单位")
    private String useDoseUnit;

    @ItemProperty(alias = "药物剂量单位")
    private String dosageUnit;

    @ItemProperty(alias = "平台药物使用频率代码")
    private String usingRate;

    @ItemProperty(alias = "平台药物使用途径代码")
    private String usePathways;

    @ItemProperty(alias = "使用频率id")
    private String usingRateId;

    @ItemProperty(alias = "用药途径id")
    private String usePathwaysId;

    @ItemProperty(alias = "机构的频次代码")
    private String organUsingRate;

    @ItemProperty(alias = "机构的用法代码")
    private String organUsePathways;

    @ItemProperty(alias = "用药频率说明, 防止覆盖原有usingRateText")
    private String usingRateTextFromHis;

    @ItemProperty(alias = "用药方式说明")
    private String usePathwaysTextFromHis;

    @ItemProperty(alias = "药物使用总数量")
    private Double useTotalDose;

    @ItemProperty(alias = "药物使用天数")
    private Integer useDays;

    @ItemProperty(alias = "药物金额 = useTotalDose * salePrice")
    private BigDecimal drugCost;

    @ItemProperty(alias = "药品嘱托Id")
    private String entrustmentId;

    @ItemProperty(alias = "药品嘱托信息")
    private String memo;

    @ItemProperty(alias = "药品嘱托编码")
    private String drugEntrustCode;

    @ItemProperty(alias = "药品效期")
    private Date validDate;

    @ItemProperty(alias = "销售价格 = organDrug.salePrice")
    private BigDecimal salePrice;

    @ItemProperty(alias = "药品编码")
    private String drugCode;

    @ItemProperty(alias = "是否启用")
    private Integer status;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    @ItemProperty(alias = "生产厂家")
    private String producer;

    @ItemProperty(alias = "生产厂家代码")
    private String producerCode;

    @ItemProperty(alias = "药物使用天数(小数类型)")
    private String useDaysB;

    @ItemProperty(alias = "处方药品详情类型")
    private Integer drugType;

    @ItemProperty(alias = "药品超量编码")
    private String superScalarCode;

    @ItemProperty(alias = "药品超量名称")
    private String superScalarName;

    @ItemProperty(alias = "医保药品编码")
    private String medicalDrugCode;

    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;

    @ItemProperty(alias = "药房名称")
    private String pharmacyName;

    @ItemProperty(alias = "前端展示的药品拼接名")
    private String drugDisplaySplicedName;

    @ItemProperty(alias = "前端展示的商品拼接名")
    private String drugDisplaySplicedSaleName;

    @ItemProperty(alias = "单个药品医保类型 医保审批类型 0自费 1医保（默认0） 前端控制传入")
    private Integer drugMedicalFlag;

    @ItemProperty(alias = "类型：1:药品，2:诊疗项目，3....")
    private Integer type;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column
    public Integer getFastRecipeId() {
        return fastRecipeId;
    }

    public void setFastRecipeId(Integer fastRecipeId) {
        this.fastRecipeId = fastRecipeId;
    }

    @Column
    public String getSaleName() {
        return saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    @Column
    public Integer getDrugId() {
        return drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    @Column
    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    @Column
    public String getDrugItemCode() {
        return drugItemCode;
    }

    public void setDrugItemCode(String drugItemCode) {
        this.drugItemCode = drugItemCode;
    }

    @Column
    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    @Column
    public String getDrugSpec() {
        return drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    @Column
    public Integer getPack() {
        return pack;
    }

    public void setPack(Integer pack) {
        this.pack = pack;
    }

    @Column
    public String getDrugUnit() {
        return drugUnit;
    }

    public void setDrugUnit(String drugUnit) {
        this.drugUnit = drugUnit;
    }

    @Column
    public Double getUseDose() {
        return useDose;
    }

    public void setUseDose(Double useDose) {
        this.useDose = useDose;
    }

    @Column
    public Double getDefaultUseDose() {
        return defaultUseDose;
    }

    public void setDefaultUseDose(Double defaultUseDose) {
        this.defaultUseDose = defaultUseDose;
    }

    @Column
    public String getUseDoseStr() {
        return useDoseStr;
    }

    public void setUseDoseStr(String useDoseStr) {
        this.useDoseStr = useDoseStr;
    }

    @Column
    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    @Column
    public String getDosageUnit() {
        return dosageUnit;
    }

    public void setDosageUnit(String dosageUnit) {
        this.dosageUnit = dosageUnit;
    }

    @Column
    public String getUsingRate() {
        return usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    @Column
    public String getUsePathways() {
        return usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    @Column
    public String getUsingRateId() {
        return usingRateId;
    }

    public void setUsingRateId(String usingRateId) {
        this.usingRateId = usingRateId;
    }

    @Column
    public String getUsePathwaysId() {
        return usePathwaysId;
    }

    public void setUsePathwaysId(String usePathwaysId) {
        this.usePathwaysId = usePathwaysId;
    }

    @Column
    public String getOrganUsingRate() {
        return organUsingRate;
    }

    public void setOrganUsingRate(String organUsingRate) {
        this.organUsingRate = organUsingRate;
    }

    @Column
    public String getOrganUsePathways() {
        return organUsePathways;
    }

    public void setOrganUsePathways(String organUsePathways) {
        this.organUsePathways = organUsePathways;
    }

    @Column
    public String getUsingRateTextFromHis() {
        return usingRateTextFromHis;
    }

    public void setUsingRateTextFromHis(String usingRateTextFromHis) {
        this.usingRateTextFromHis = usingRateTextFromHis;
    }

    @Column
    public String getUsePathwaysTextFromHis() {
        return usePathwaysTextFromHis;
    }

    public void setUsePathwaysTextFromHis(String usePathwaysTextFromHis) {
        this.usePathwaysTextFromHis = usePathwaysTextFromHis;
    }

    @Column
    public Double getUseTotalDose() {
        return useTotalDose;
    }

    public void setUseTotalDose(Double useTotalDose) {
        this.useTotalDose = useTotalDose;
    }

    @Column
    public Integer getUseDays() {
        return useDays;
    }

    public void setUseDays(Integer useDays) {
        this.useDays = useDays;
    }

    @Column
    public BigDecimal getDrugCost() {
        return drugCost;
    }

    public void setDrugCost(BigDecimal drugCost) {
        this.drugCost = drugCost;
    }

    @Column
    public String getEntrustmentId() {
        return entrustmentId;
    }

    public void setEntrustmentId(String entrustmentId) {
        this.entrustmentId = entrustmentId;
    }

    @Column
    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Column
    public String getDrugEntrustCode() {
        return drugEntrustCode;
    }

    public void setDrugEntrustCode(String drugEntrustCode) {
        this.drugEntrustCode = drugEntrustCode;
    }

    @Column
    public Date getValidDate() {
        return validDate;
    }

    public void setValidDate(Date validDate) {
        this.validDate = validDate;
    }

    @Column
    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    @Column
    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    @Column
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column
    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }

    @Column
    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    @Column
    public String getProducerCode() {
        return producerCode;
    }

    public void setProducerCode(String producerCode) {
        this.producerCode = producerCode;
    }

    @Column
    public String getUseDaysB() {
        return useDaysB;
    }

    public void setUseDaysB(String useDaysB) {
        this.useDaysB = useDaysB;
    }

    @Column
    public Integer getDrugType() {
        return drugType;
    }

    public void setDrugType(Integer drugType) {
        this.drugType = drugType;
    }

    @Column
    public String getSuperScalarCode() {
        return superScalarCode;
    }

    public void setSuperScalarCode(String superScalarCode) {
        this.superScalarCode = superScalarCode;
    }

    @Column
    public String getSuperScalarName() {
        return superScalarName;
    }

    public void setSuperScalarName(String superScalarName) {
        this.superScalarName = superScalarName;
    }

    @Column
    public String getMedicalDrugCode() {
        return medicalDrugCode;
    }

    public void setMedicalDrugCode(String medicalDrugCode) {
        this.medicalDrugCode = medicalDrugCode;
    }

    @Column
    public Integer getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(Integer pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    @Column
    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    @Column
    public String getDrugDisplaySplicedName() {
        return drugDisplaySplicedName;
    }

    public void setDrugDisplaySplicedName(String drugDisplaySplicedName) {
        this.drugDisplaySplicedName = drugDisplaySplicedName;
    }

    @Column
    public String getDrugDisplaySplicedSaleName() {
        return drugDisplaySplicedSaleName;
    }

    public void setDrugDisplaySplicedSaleName(String drugDisplaySplicedSaleName) {
        this.drugDisplaySplicedSaleName = drugDisplaySplicedSaleName;
    }

    @Column
    public Integer getDrugMedicalFlag() {
        return drugMedicalFlag;
    }

    public void setDrugMedicalFlag(Integer drugMedicalFlag) {
        this.drugMedicalFlag = drugMedicalFlag;
    }

    @Column
    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
