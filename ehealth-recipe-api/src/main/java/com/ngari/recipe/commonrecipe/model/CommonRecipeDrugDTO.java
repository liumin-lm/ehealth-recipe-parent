package com.ngari.recipe.commonrecipe.model;

import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Created by  on 2017/5/23.
 *
 * @author jiangtingfeng
 */
@Schema
public class CommonRecipeDrugDTO implements java.io.Serializable {

    private static final long serialVersionUID = -4535607360492071383L;

    @ItemProperty(alias = "自增id")
    private Integer id;

    @ItemProperty(alias = "药品状态")
    private Integer drugStatus;

    @ItemProperty(alias = "常用方Id")
    private Integer commonRecipeId;

    @ItemProperty(alias = "药品ID")
    private Integer drugId;

    @ItemProperty(alias = "机构药品编码")
    private String organDrugCode;

    @ItemProperty(alias = "药物名称")
    private String drugName;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias = "药物单位")
    private String drugUnit;

    @ItemProperty(alias = "药物规格")
    private String drugSpec;

    @ItemProperty(alias = "药物使用总数量")
    private Double useTotalDose;

    @ItemProperty(alias = "药物使用次剂量")
    private Double useDose;

    @ItemProperty(alias="药物使用次剂量--中文标识-适量")
    private String useDoseStr;

    @ItemProperty(alias = "默认每次剂量")
    private Double defaultUseDose;

    @ItemProperty(alias = "销售价格")
    private BigDecimal salePrice;

    @ItemProperty(alias = "销售单价")
    private Double price1;

    @ItemProperty(alias = "总药物金额")
    private BigDecimal drugCost;

    @ItemProperty(alias = "备注信息")
    private String memo;

    @ItemProperty(alias = "药品嘱托编码")
    private String drugEntrustCode ;

    @ItemProperty(alias = "药品嘱托Id")
    private String drugEntrustId ;


    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "药物使用频率代码")
    @Dictionary(id = "eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias = "使用频率id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsingRate")
    private String usingRateId;
    @ItemProperty(alias = "用药频次英文名称")
    private String usingRateEnglishNames;

    @ItemProperty(alias = "药物使用途径代码")
    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias = "用药途径id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsePathways")
    private String usePathwaysId;
    @ItemProperty(alias = "用药途径英文名称")
    private String usePathEnglishNames;

    @ItemProperty(alias = "药物使用天数")
    private Integer useDays;

    @ItemProperty(alias = "剂量单位")
    private String useDoseUnit;

    @ItemProperty(alias = "平台商品名")
    private String platformSaleName;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    @ItemProperty(alias = "医生端选择的每次剂量和单位绑定关系")
    private List<UseDoseAndUnitRelationBean> useDoseAndUnitRelation;

    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;

    public String getDrugEntrustCode() {
        return drugEntrustCode;
    }

    public void setDrugEntrustCode(String drugEntrustCode) {
        this.drugEntrustCode = drugEntrustCode;
    }

    public String getDrugEntrustId() {
        return drugEntrustId;
    }

    public void setDrugEntrustId(String drugEntrustId) {
        this.drugEntrustId = drugEntrustId;
    }

    @ItemProperty(alias = "药房名称")
    private String pharmacyName;

    @ItemProperty(alias = "药品包装数量")
    private Integer pack;

    @ItemProperty(alias = "机构药品药房id")
    private String organPharmacyId;

    @ItemProperty(alias = "中药禁忌类型(1:超量 2:十八反 3:其它)")
    private Integer tcmContraindicationType;

    @ItemProperty(alias = "中药禁忌原因")
    private String tcmContraindicationCause;

    @ItemProperty(alias = "前端展示的药品拼接名")
    private String drugDisplaySplicedName;

    @ItemProperty(alias = "前端展示的商品拼接名")
    private String drugDisplaySplicedSaleName;



    public Integer getPack() {
        return pack;
    }

    public void setPack(Integer pack) {
        this.pack = pack;
    }

    public CommonRecipeDrugDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDrugStatus() {
        return drugStatus;
    }

    public void setDrugStatus(Integer drugStatus) {
        this.drugStatus = drugStatus;
    }

    public Integer getCommonRecipeId() {
        return commonRecipeId;
    }

    public void setCommonRecipeId(Integer commonRecipeId) {
        this.commonRecipeId = commonRecipeId;
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

    public String getDrugUnit() {
        return drugUnit;
    }

    public void setDrugUnit(String drugUnit) {
        this.drugUnit = drugUnit;
    }

    public String getDrugSpec() {
        return drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    public Double getUseTotalDose() {
        return useTotalDose;
    }

    public void setUseTotalDose(Double useTotalDose) {
        this.useTotalDose = useTotalDose;
    }

    public Double getUseDose() {
        return useDose;
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

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public Double getPrice1() {
        return price1;
    }

    public void setPrice1(Double price1) {
        this.price1 = price1;
    }

    public BigDecimal getDrugCost() {
        return drugCost;
    }

    public void setDrugCost(BigDecimal drugCost) {
        this.drugCost = drugCost;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
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

    public Integer getUseDays() {
        return useDays;
    }

    public void setUseDays(Integer useDays) {
        this.useDays = useDays;
    }

    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    public String getSaleName() {
        return saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    public String getPlatformSaleName() {
        return platformSaleName;
    }

    public void setPlatformSaleName(String platformSaleName) {
        this.platformSaleName = platformSaleName;
    }

    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
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

    public String getOrganPharmacyId() {
        return organPharmacyId;
    }

    public void setOrganPharmacyId(String organPharmacyId) {
        this.organPharmacyId = organPharmacyId;
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

    public String getUsingRateEnglishNames() {
        return usingRateEnglishNames;
    }

    public void setUsingRateEnglishNames(String usingRateEnglishNames) {
        this.usingRateEnglishNames = usingRateEnglishNames;
    }

    public String getUsePathEnglishNames() {
        return usePathEnglishNames;
    }

    public void setUsePathEnglishNames(String usePathEnglishNames) {
        this.usePathEnglishNames = usePathEnglishNames;
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
}
