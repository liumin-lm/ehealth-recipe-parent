package com.ngari.recipe.entity;


import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 药品目录
 *
 * @author <a href="mailto:luf@ngarihealth.com">luf</a>
 */
@Entity
@Schema
@Table(name = "base_druglist")
@Access(AccessType.PROPERTY)
public class DrugList implements java.io.Serializable {
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

    @ItemProperty(alias = "剂量单位")
    private String useDoseUnit;

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

    @ItemProperty(alias = "药品本位码")
    private String standardCode;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    @ItemProperty(alias = "机构药品编码")
    private String organDrugCode;

    @ItemProperty(alias = "基药标识")
    private Integer baseDrug;

    @ItemProperty(alias = "药品编码")
    private String drugCode;

    @ItemProperty(alias = "标志（浙江），1-是，0-否")
    private Integer isRegulation;


    @ItemProperty(alias = "标志（审方），1-是，0-否")
    private Integer isPrescriptions;

    @ItemProperty(alias = "是否标准药品，1-是，0-否")
    private Integer isStandardDrug=0;

    @ItemProperty(alias = "来源渠道名称")
    private String sourceOrganText;



    public DrugList() {
    }

    public DrugList(Integer drugId) {
        this.drugId = drugId;
    }

    public DrugList(Integer drugId, String drugName, String saleName,
                    String drugSpec, String unit, Integer drugType, String drugClass,
                    Double useDose, String useDoseUnit, String usingRate,
                    String usePathways, String producer, Integer instructions,
                    String drugPic, Double price1, Double price2, Integer status) {
        this.drugId = drugId;
        this.drugName = drugName;
        this.saleName = saleName;
        this.drugSpec = drugSpec;
        this.unit = unit;
        this.drugType = drugType;
        this.drugClass = drugClass;
        this.useDose = useDose;
        this.useDoseUnit = useDoseUnit;
        this.usingRate = usingRate;
        this.usePathways = usePathways;
        this.producer = producer;
        this.instructions = instructions;
        this.drugPic = drugPic;
        this.price1 = price1;
        this.price2 = price2;
        this.status = status;
    }

    public DrugList(Integer drugId, String drugName,
                    String saleName, String drugSpec,
                    Integer pack, String unit,
                    Integer drugType, String drugClass,
                    Double useDose, String useDoseUnit,
                    String usingRate, String usePathways,
                    String producer, Integer instructions,
                    String drugPic, Double price1,
                    Double price2, Integer status,
                    String indications, String pyCode, Date createDt,
                    Date lastModify, String allPyCode,
                    String approvalNumber) {
        this.drugId = drugId;
        this.drugName = drugName;
        this.saleName = saleName;
        this.drugSpec = drugSpec;
        this.pack = pack;
        this.unit = unit;
        this.drugType = drugType;
        this.drugClass = drugClass;
        this.useDose = useDose;
        this.useDoseUnit = useDoseUnit;
        this.usingRate = usingRate;
        this.usePathways = usePathways;
        this.producer = producer;
        this.instructions = instructions;
        this.drugPic = drugPic;
        this.price1 = price1;
        this.price2 = price2;
        this.status = status;
        this.indications = indications;
        this.pyCode = pyCode;
        this.createDt = createDt;
        this.lastModify = lastModify;
        this.allPyCode = allPyCode;
        this.approvalNumber = approvalNumber;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "DrugId", unique = true, nullable = false)
    public Integer getDrugId() {
        return this.drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    @Column(name = "DrugName", length = 50)
    public String getDrugName() {
        return this.drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    @Column(name = "SaleName", length = 50)
    public String getSaleName() {
        return this.saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    @Column(name = "DrugSpec", length = 50)
    public String getDrugSpec() {
        return this.drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    @Column(name = "Unit", length = 6)
    public String getUnit() {
        return this.unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Column(name = "DrugType")
    public Integer getDrugType() {
        return this.drugType;
    }

    public void setDrugType(Integer drugType) {
        this.drugType = drugType;
    }

    @Column(name = "DrugClass", length = 20)
    public String getDrugClass() {
        return this.drugClass;
    }

    public void setDrugClass(String drugClass) {
        this.drugClass = drugClass;
    }

    @Column(name = "UseDose", precision = 10, scale = 3)
    public Double getUseDose() {
        return this.useDose;
    }

    public void setUseDose(Double useDose) {
        this.useDose = useDose;
    }

    @Column(name = "UseDoseUnit", length = 6)
    public String getUseDoseUnit() {
        return this.useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    @Column(name = "UsingRate", length = 10)
    public String getUsingRate() {
        return this.usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    @Column(name = "UsePathways", length = 10)
    public String getUsePathways() {
        return this.usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    @Column(name = "Producer", length = 20)
    public String getProducer() {
        return this.producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    @Column(name = "Instructions")
    public Integer getInstructions() {
        return this.instructions;
    }

    public void setInstructions(Integer instructions) {
        this.instructions = instructions;
    }

    @Column(name = "DrugPic")
    public String getDrugPic() {
        return this.drugPic;
    }

    public void setDrugPic(String drugPic) {
        this.drugPic = drugPic;
    }

    @Column(name = "Price1", precision = 10)
    public Double getPrice1() {
        return this.price1;
    }

    public void setPrice1(Double price1) {
        this.price1 = price1;
    }

    @Column(name = "Price2", precision = 10)
    public Double getPrice2() {
        return this.price2;
    }

    public void setPrice2(Double price2) {
        this.price2 = price2;
    }

    @Column(name = "Status")
    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "Indications", length = 50)
    public String getIndications() {
        return this.indications;
    }

    public void setIndications(String indications) {
        this.indications = indications;
    }

    @Column(name = "PyCode", length = 20)
    public String getPyCode() {
        return this.pyCode;
    }

    public void setPyCode(String pyCode) {
        this.pyCode = pyCode;
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

    @Column(name = "AllPyCode", nullable = false, length = 255)
    public String getAllPyCode() {
        return allPyCode;
    }

    public void setAllPyCode(String allPyCode) {
        this.allPyCode = allPyCode;
    }

    @Column(name = "Pack", nullable = false, length = 11)
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

    @Transient
    public BigDecimal getHospitalPrice() {
        return hospitalPrice;
    }

    public void setHospitalPrice(BigDecimal hospitalPrice) {
        this.hospitalPrice = hospitalPrice;
    }

    @Transient
    public String getHighlightedField() {
        return highlightedField;
    }

    public void setHighlightedField(String highlightedField) {
        this.highlightedField = highlightedField;
    }

    @Transient
    public List getHighlightedFieldForIos() {
        return highlightedFieldForIos;
    }

    public void setHighlightedFieldForIos(List highlightedFieldForIos) {
        this.highlightedFieldForIos = highlightedFieldForIos;
    }

    @Column(name = "SourceOrgan")
    public Integer getSourceOrgan() {
        return sourceOrgan;
    }

    public void setSourceOrgan(Integer sourceOrgan) {
        this.sourceOrgan = sourceOrgan;
    }

    @Transient
    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    @Column(name = "standardCode")
    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
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

    @Column(name = "drugCode")
    public String getDrugCode() {
        return drugCode;
}

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
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

    @Column(name = "isRegulation")
    public Integer getIsRegulation() {
        return isRegulation;
    }

    public void setIsRegulation(Integer isRegulation) {
        this.isRegulation = isRegulation;
    }

    @Column(name = "isPrescriptions")
    public Integer getIsPrescriptions() {
        return isPrescriptions;
    }

    public void setIsPrescriptions(Integer isPrescriptions) {
        this.isPrescriptions = isPrescriptions;
    }

    @Column(name = "isStandardDrug")
    public Integer getIsStandardDrug() {
        return isStandardDrug;
    }

    public void setIsStandardDrug(Integer isStandardDrug) {
        this.isStandardDrug = isStandardDrug;
    }

    @Transient
    public String getSourceOrganText() {
        return sourceOrganText;
    }

    public void setSourceOrganText(String sourceOrganText) {
        this.sourceOrganText = sourceOrganText;
    }
}