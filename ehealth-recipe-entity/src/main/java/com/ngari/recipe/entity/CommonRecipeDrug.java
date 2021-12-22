package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Created by  on 2017/5/23.
 * @author jiangtingfeng
 */
@Schema
@Entity
@Table(name = "cdr_commonRecipeDrug")
@Access(AccessType.PROPERTY)
public class CommonRecipeDrug implements java.io.Serializable{

    private static final long serialVersionUID = -5511208206035918004L;

    @ItemProperty(alias="自增id")
    private Integer id;

    @ItemProperty(alias="药品状态")
    private Integer drugStatus;

    @ItemProperty(alias="常用方Id")
    private Integer commonRecipeId;

    @ItemProperty(alias="药品ID")
    private Integer drugId;

    @ItemProperty(alias = "机构药品编码")
    private String organDrugCode;

    @ItemProperty(alias="药物名称")
    private String drugName;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias="药物单位")
    private String drugUnit;

    @ItemProperty(alias="药物规格")
    private String drugSpec;

    @ItemProperty(alias="药物使用总数量")
    private Double useTotalDose;

    @ItemProperty(alias="药物使用次剂量")
    private Double useDose;

    @ItemProperty(alias="药物使用次剂量--中文标识-适量")
    private String useDoseStr;

    @ItemProperty(alias="默认每次剂量")
    private Double defaultUseDose;

    @ItemProperty(alias="销售价格")
    private BigDecimal salePrice;

    @ItemProperty(alias="销售单价")
    private Double price1;

    @ItemProperty(alias="总药物金额")
    private BigDecimal drugCost;

    @ItemProperty(alias="药品嘱托信息")
    private String memo;

    @ItemProperty(alias = "药品嘱托编码")
    private String drugEntrustCode ;

    @ItemProperty(alias = "药品嘱托Id")
    private String drugEntrustId ;

    @ItemProperty(alias="药物使用频率代码")
    @Dictionary(id="eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias="药物使用途径代码")
    @Dictionary(id="eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias="药物使用天数")
    private Integer useDays;

    @ItemProperty(alias="剂量单位")
    private String useDoseUnit;

    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;
    @ItemProperty(alias = "药房名称")
    private String pharmacyName;

    @ItemProperty(alias = "使用频率id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsingRate")
    private String usingRateId;

    @ItemProperty(alias = "用药途径id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsePathways")
    private String usePathwaysId;

    @ItemProperty(alias = "药品包装数量")
    private Integer pack;


    @ItemProperty(alias = "中药禁忌类型(1:超量 2:十八反 3:其它)")
    private Integer tcmContraindicationType;

    @ItemProperty(alias = "中药禁忌原因")
    private String tcmContraindicationCause;

    @ItemProperty(alias = "前端展示的药品拼接名")
    private String drugDisplaySplicedName;

    @ItemProperty(alias = "前端展示的商品拼接名")
    private String drugDisplaySplicedSaleName;

    @ItemProperty(alias = "药品超量编码")
    private String superScalarCode;

    @ItemProperty(alias = "药品超量名称")
    private String superScalarName;

    @Column(name = "pack")
    public Integer getPack() {
        return pack;
    }

    public void setPack(Integer pack) {
        this.pack = pack;
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

    @Column(name = "UseDoseUnit")
    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    @Transient
    public Integer getDrugStatus() {
        return drugStatus;
    }

    public void setDrugStatus(Integer drugStatus) {
        this.drugStatus = drugStatus;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id" ,nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "UsePathways")
    public String getUsePathways() {
        return usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    @Column(name = "DefaultUseDose")
    public Double getDefaultUseDose() {
        return defaultUseDose;
    }

    public void setDefaultUseDose(Double defaultUseDose) {
        this.defaultUseDose = defaultUseDose;
    }

    @Column(name = "UseDays")
    public Integer getUseDays() {
        return useDays;
    }

    public void setUseDays(Integer useDays) {
        this.useDays = useDays;
    }

    @Column(name = "DrugName")
    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    @Column(name = "DrugUnit")
    public String getDrugUnit() {
        return drugUnit;
    }

    public void setDrugUnit(String drugUnit) {
        this.drugUnit = drugUnit;
    }

    @Column(name = "DrugSpec")
    public String getDrugSpec() {
        return drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    @Column(name = "UseTotalDose")
    public Double getUseTotalDose() {
        return useTotalDose;
    }

    public void setUseTotalDose(Double useTotalDose) {
        this.useTotalDose = useTotalDose;
    }

    @Column(name = "UseDose")
    public Double getUseDose() {
        return useDose;
    }

    public void setUseDose(Double useDose) {
        this.useDose = useDose;
    }

    @Column(name = "SalePrice")
    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    @Transient
    public Double getPrice1() {
        return price1;
    }

    public void setPrice1(Double price1) {
        this.price1 = price1;
    }

    @Column(name = "DrugCost")
    public BigDecimal getDrugCost() {
        return drugCost;
    }

    public void setDrugCost(BigDecimal drugCost) {
        this.drugCost = drugCost;
    }

    @Column(name = "Memo")
    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Column(name = "UsingRate")
    public String getUsingRate() {
        return usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    @Column(name = "CommonRecipeId")
    public Integer getCommonRecipeId() {
        return commonRecipeId;
    }

    public void setCommonRecipeId(Integer commonRecipeId) {
        this.commonRecipeId = commonRecipeId;
    }

    @Column(name = "DrugId")
    public Integer getDrugId() {
        return drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    @Column(name = "organDrugCode")
    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    @Column(name = "saleName")
    public String getSaleName() {
        return saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    @Column(name = "useDoseStr")
    public String getUseDoseStr() {
        return useDoseStr;
    }

    public void setUseDoseStr(String useDoseStr) {
        this.useDoseStr = useDoseStr;
    }

    @Column(name = "tcm_contraindication_type")
    public Integer getTcmContraindicationType() {
        return tcmContraindicationType;
    }

    public void setTcmContraindicationType(Integer tcmContraindicationType) {
        this.tcmContraindicationType = tcmContraindicationType;
    }

    @Column(name = "tcm_contraindication_cause")
    public String getTcmContraindicationCause() {
        return tcmContraindicationCause;
    }

    public void setTcmContraindicationCause(String tcmContraindicationCause) {
        this.tcmContraindicationCause = tcmContraindicationCause;
    }

    @Override
    public String toString() {
        return "CommonRecipeDrug{" +
                "id=" + id +
                ", drugStatus=" + drugStatus +
                ", commonRecipeId=" + commonRecipeId +
                ", drugId=" + drugId +
                ", organDrugCode='" + organDrugCode + '\'' +
                ", drugName='" + drugName + '\'' +
                ", saleName='" + saleName + '\'' +
                ", drugUnit='" + drugUnit + '\'' +
                ", drugSpec='" + drugSpec + '\'' +
                ", useTotalDose=" + useTotalDose +
                ", useDose=" + useDose +
                ", useDoseStr='" + useDoseStr + '\'' +
                ", defaultUseDose=" + defaultUseDose +
                ", salePrice=" + salePrice +
                ", price1=" + price1 +
                ", drugCost=" + drugCost +
                ", memo='" + memo + '\'' +
                ", usingRate='" + usingRate + '\'' +
                ", usePathways='" + usePathways + '\'' +
                ", useDays=" + useDays +
                ", useDoseUnit='" + useDoseUnit + '\'' +
                ", pharmacyId=" + pharmacyId +
                ", pharmacyName='" + pharmacyName + '\'' +
                ", usingRateId='" + usingRateId + '\'' +
                ", usePathwaysId='" + usePathwaysId + '\'' +
                ", pack=" + pack +
                '}';
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

    @Column(name = "drugEntrustCode")
    public String getDrugEntrustCode() {
        return drugEntrustCode;
    }

    public void setDrugEntrustCode(String drugEntrustCode) {
        this.drugEntrustCode = drugEntrustCode;
    }

    @Column(name = "drugEntrustId")
    public String getDrugEntrustId() {
        return drugEntrustId;
    }

    public void setDrugEntrustId(String drugEntrustId) {
        this.drugEntrustId = drugEntrustId;
    }

    @Column(name = "super_scalar_code")
    public String getSuperScalarCode() {
        return superScalarCode;
    }

    public void setSuperScalarCode(String superScalarCode) {
        this.superScalarCode = superScalarCode;
    }

    @Column(name = "super_scalar_name")
    public String getSuperScalarName() {
        return superScalarName;
    }

    public void setSuperScalarName(String superScalarName) {
        this.superScalarName = superScalarName;
    }
}
