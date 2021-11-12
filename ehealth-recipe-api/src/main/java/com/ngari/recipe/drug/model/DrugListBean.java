package com.ngari.recipe.drug.model;


import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import ctd.util.JSONUtils;

import javax.persistence.Transient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 药品目录
 * @author yuyun
 */
@Schema
public class DrugListBean implements IDrugInventory, Serializable {

    public static final long serialVersionUID = -3983203173007645688L;

    @ItemProperty(alias = "药品序号")
    private Integer drugId;

    @ItemProperty(alias = "药品名称")
    private String drugName;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias = "药品规格")
    private String drugSpec;

    @ItemProperty(alias = "药品包装数量")
    private Integer pack;

    @ItemProperty(alias = "药品单位")
    private String unit;

    @ItemProperty(alias = "药品类型")
    @Dictionary(id = "eh.base.dictionary.DrugType")
    private Integer drugType;

    @ItemProperty(alias = "药品分类")
    @Dictionary(id = "eh.base.dictionary.DrugClass")
    private String drugClass;

    @ItemProperty(alias = "一次剂量")
    private Double useDose;

    @ItemProperty(alias = "推荐单次剂量")
    private Double recommendedUseDose;

    @ItemProperty(alias = "剂量单位")
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

    @ItemProperty(alias = "药品说明书")
    private Integer instructions;

    @ItemProperty(alias = "药品图片")
    private String drugPic;

    @ItemProperty(alias = "参考价格1")
    private Double price1;

    @ItemProperty(alias = "参考价格2")
    private Double price2;

    @ItemProperty(alias = "院内销售价格")
    private BigDecimal salePrice;

    @ItemProperty(alias = "使用状态")
    @Dictionary(id = "eh.base.dictionary.DrugListStatus")
    private Integer status;

    @ItemProperty(alias = "药品来源机构，null表示基础库数据")
    private Integer sourceOrgan;

    @ItemProperty(alias = "适用症状")
    private String indications;

    @ItemProperty(alias = "拼音码")
    private String pyCode;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "商品名全拼")
    private String allPyCode;

    @ItemProperty(alias = "批准文号")
    private String approvalNumber;

    @ItemProperty(alias = "高亮字段")
    private String highlightedField;

    @ItemProperty(alias = "高亮字段给ios用")
    private List highlightedFieldForIos;

    @ItemProperty(alias = "医院价格")
    private BigDecimal hospitalPrice;

    @ItemProperty(alias = "机构药品编码")
    private String organDrugCode;

    private DispensatoryDTO dispensatory;

    @ItemProperty(alias = "是否是匹配的药品(药品工具返回前端用)")
    private boolean isMatched = false;

    @ItemProperty(alias = "基药标识")
    private Integer baseDrug;

    @ItemProperty(alias = "药品编码")
    private String drugCode;
    @ItemProperty(alias = "药品库存")
    private BigDecimal inventory;
    @ItemProperty(alias = "剂型")
    private String drugForm;
    @ItemProperty(alias = "药品库存标志")
    private boolean drugInventoryFlag;

    @ItemProperty(alias = "药品本位码")
    private String standardCode;

    @ItemProperty(alias = "医生端选择的每次剂量和单位绑定关系")
    private List<UseDoseAndUnitRelationBean> useDoseAndUnitRelation;

    @ItemProperty(alias = "标志（浙江），1-是，0-否")
    private Integer isRegulation;


    @ItemProperty(alias = "标志（审方），1-是，0-否")
    private Integer isPrescriptions;


    @ItemProperty(alias = "医院库存、药企库存等列表信息")
    private List<DrugInventoryInfo> inventories;

    @ItemProperty(alias = "his药品商保保险比例")
    private String hisciReimburseRate;

    @ItemProperty(alias = "his药品是否商保标识")
    private Integer hisciIsClaim;

    @ItemProperty(alias = "来源渠道名称")
    private String sourceOrganText;

    @ItemProperty(alias = "前端展示的药品名拼接名")
    private String drugDisplaySplicedName;
    @ItemProperty(alias = "前端展示的药品商品名拼接名")
    private String drugDisplaySplicedSaleName;

    @ItemProperty(alias = "库存有无")
    private boolean inventoriesFlag;

    @ItemProperty(alias = "药品嘱托")
    private String drugEntrust;

    @ItemProperty(alias = "药品嘱托编码")
    private String drugEntrustCode;

    @ItemProperty(alias = "药品嘱托Id")
    private String drugEntrustId;

    @ItemProperty(alias = "是否标准药品，1-是，0-否")
    private Integer isStandardDrug;

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

    public String getDrugEntrust() {
        return drugEntrust;
    }

    public void setDrugEntrust(String drugEntrust) {
        this.drugEntrust = drugEntrust;
    }

    public DrugListBean() {
    }

    public DrugListBean(Integer drugId) {
        this.drugId = drugId;
    }

    public String getHisciReimburseRate() {
        return hisciReimburseRate;
    }

    public void setHisciReimburseRate(String hisciReimburseRate) {
        this.hisciReimburseRate = hisciReimburseRate;
    }

    public Integer getHisciIsClaim() {
        return hisciIsClaim;
    }

    public void setHisciIsClaim(Integer hisciIsClaim) {
        this.hisciIsClaim = hisciIsClaim;
    }

    public Integer getBaseDrug() {
        return baseDrug;
    }

    public void setBaseDrug(Integer baseDrug) {
        this.baseDrug = baseDrug;
    }

    public boolean getIsMatched() {
        return isMatched;
    }

    public void setIsMatched(boolean matched) {
        isMatched = matched;
    }

    @Override
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

    public Integer getDrugType() {
        return drugType;
    }

    public void setDrugType(Integer drugType) {
        this.drugType = drugType;
    }

    public String getDrugClass() {
        return drugClass;
    }

    public void setDrugClass(String drugClass) {
        this.drugClass = drugClass;
    }

    public Double getUseDose() {
        return useDose;
    }

    public void setUseDose(Double useDose) {
        this.useDose = useDose;
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

    public Integer getInstructions() {
        return instructions;
    }

    public void setInstructions(Integer instructions) {
        this.instructions = instructions;
    }

    public String getDrugPic() {
        return drugPic;
    }

    public void setDrugPic(String drugPic) {
        this.drugPic = drugPic;
    }

    public Double getPrice1() {
        return price1;
    }

    public void setPrice1(Double price1) {
        this.price1 = price1;
    }

    public Double getPrice2() {
        return price2;
    }

    public void setPrice2(Double price2) {
        this.price2 = price2;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getIndications() {
        return indications;
    }

    public void setIndications(String indications) {
        this.indications = indications;
    }

    public String getPyCode() {
        return pyCode;
    }

    public void setPyCode(String pyCode) {
        this.pyCode = pyCode;
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

    public String getAllPyCode() {
        return allPyCode;
    }

    public void setAllPyCode(String allPyCode) {
        this.allPyCode = allPyCode;
    }

    public String getApprovalNumber() {
        return approvalNumber;
    }

    public void setApprovalNumber(String approvalNumber) {
        this.approvalNumber = approvalNumber;
    }

    public String getHighlightedField() {
        return highlightedField;
    }

    public void setHighlightedField(String highlightedField) {
        this.highlightedField = highlightedField;
    }

    public List getHighlightedFieldForIos() {
        return highlightedFieldForIos;
    }

    public void setHighlightedFieldForIos(List highlightedFieldForIos) {
        this.highlightedFieldForIos = highlightedFieldForIos;
    }

    public BigDecimal getHospitalPrice() {
        return hospitalPrice;
    }

    public void setHospitalPrice(BigDecimal hospitalPrice) {
        this.hospitalPrice = hospitalPrice;
    }

    public DispensatoryDTO getDispensatory() {
        return dispensatory;
    }

    public void setDispensatory(DispensatoryDTO dispensatory) {
        this.dispensatory = dispensatory;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    @Override
    public String toString() {
        return JSONUtils.toString(this);
    }

    @Override
    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    public BigDecimal getInventory() {
        return inventory;
    }

    public void setInventory(BigDecimal inventory) {
        this.inventory = inventory;
    }

    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }

    public boolean isDrugInventoryFlag() {
        return drugInventoryFlag;
    }

    public void setDrugInventoryFlag(boolean drugInventoryFlag) {
        this.drugInventoryFlag = drugInventoryFlag;
    }

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public Integer getSourceOrgan() {
        return sourceOrgan;
    }

    public void setSourceOrgan(Integer sourceOrgan) {
        this.sourceOrgan = sourceOrgan;
    }

    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
    }

    public Double getRecommendedUseDose() {
        return recommendedUseDose;
    }

    public void setRecommendedUseDose(Double recommendedUseDose) {
        this.recommendedUseDose = recommendedUseDose;
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

    public Integer getIsRegulation() {
        return isRegulation;
    }

    public void setIsRegulation(Integer isRegulation) {
        this.isRegulation = isRegulation;
    }

    public Integer getIsPrescriptions() {
        return isPrescriptions;
    }

    public void setIsPrescriptions(Integer isPrescriptions) {
        this.isPrescriptions = isPrescriptions;
    }

    public List<DrugInventoryInfo> getInventories() {
        return inventories;
    }

    @Override
    public void setInventories(List<DrugInventoryInfo> inventories) {
        this.inventories = inventories;
    }

    @Transient
    public String getSourceOrganText() {
        return sourceOrganText;
    }

    public void setSourceOrganText(String sourceOrganText) {
        this.sourceOrganText = sourceOrganText;
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

    public boolean isInventoriesFlag() {
        return inventoriesFlag;
    }

    public void setInventoriesFlag(boolean inventoriesFlag) {
        this.inventoriesFlag = inventoriesFlag;
    }

    public Integer getIsStandardDrug() {
        return isStandardDrug;
    }

    public void setIsStandardDrug(Integer isStandardDrug) {
        this.isStandardDrug = isStandardDrug;
    }
}