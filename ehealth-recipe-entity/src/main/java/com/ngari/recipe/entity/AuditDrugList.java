package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author yinsheng
 * @date 2019\5\13 0013 14:45
 */
@Entity
@Schema
@Table(name = "base_auditdruglist")
@Access(AccessType.PROPERTY)
public class AuditDrugList implements java.io.Serializable {
    private static final long serialVersionUID = 8694646487421396600L;

    @ItemProperty(alias = "审核药品序号")
    private Integer auditDrugId;

    @ItemProperty(alias = "医疗机构编码")
    private String organizeCode;

    @ItemProperty(alias = "平台机构编码")
    private Integer organId;

    @ItemProperty(alias = "医疗结构药品编码")
    private String organDrugCode;

    @ItemProperty(alias = "药品类型编码")
    private String drugClass;

    @ItemProperty(alias = "配送药品序号")
    private Integer saleDrugListId;

    @ItemProperty(alias = "机构药品序号")
    private Integer organDrugListId;

    @ItemProperty(alias = "药品名称")
    private String drugName;

    @ItemProperty(alias = "商品名称")
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

    @ItemProperty(alias = "一次剂量")
    private Double useDose;

    @ItemProperty(alias = "剂量单位")
    private String useDoseUnit;

    @ItemProperty(alias = "使用频率")
    @Dictionary(id = "eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias = "用药途径")
    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias = "生产厂家")
    private String producer;

    @ItemProperty(alias = "参考价格")
    private Double price;

    @ItemProperty(alias = "来源机构")
    private Integer sourceOrgan;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "批准文号")
    private String approvalNumber;

    @ItemProperty(alias = "药品本位码")
    private String standardCode;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    @ItemProperty(alias = "审核状态 0待审核 1审核通过 2审核拒绝")
    private Integer status;

    @ItemProperty(alias = "拒绝原因")
    private String rejectReason;

    @ItemProperty(alias = "是否已匹配 0未匹配 1已匹配")
    private Integer type;

    @ItemProperty(alias = "来源药企")
    private String sourceEnterprise;

    @ItemProperty(alias = "药品ID")
    private Integer drugId;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "AuditDrugId", unique = true, nullable = false)
    public Integer getAuditDrugId() {
        return auditDrugId;
    }

    public void setAuditDrugId(Integer auditDrugId) {
        this.auditDrugId = auditDrugId;
    }

    @Column(name = "OrganizeCode", length = 30)
    public String getOrganizeCode() {
        return organizeCode;
    }

    public void setOrganizeCode(String organizeCode) {
        this.organizeCode = organizeCode;
    }

    @Column(name = "OrganId", length = 11)
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "OrganDrugCode", length = 100)
    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    @Column(name = "SaleDrugListId", length = 11)
    public Integer getSaleDrugListId() {
        return saleDrugListId;
    }

    public void setSaleDrugListId(Integer saleDrugListId) {
        this.saleDrugListId = saleDrugListId;
    }

    @Column(name = "OrganDrugListId", length = 11)
    public Integer getOrganDrugListId() {
        return organDrugListId;
    }

    public void setOrganDrugListId(Integer organDrugListId) {
        this.organDrugListId = organDrugListId;
    }

    @Column(name = "DrugName", length = 50)
    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    @Column(name = "SaleName", length = 50)
    public String getSaleName() {
        return saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    @Column(name = "DrugClass", length = 20)
    public String getDrugClass() {
        return drugClass;
    }

    public void setDrugClass(String drugClass) {
        this.drugClass = drugClass;
    }

    @Column(name = "DrugSpec", length = 50)
    public String getDrugSpec() {
        return drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    @Column(name = "Pack", nullable = false, length = 11)
    public Integer getPack() {
        return pack;
    }

    public void setPack(Integer pack) {
        this.pack = pack;
    }

    @Column(name = "Unit", length = 6)
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Column(name = "DrugType")
    public Integer getDrugType() {
        return drugType;
    }

    public void setDrugType(Integer drugType) {
        this.drugType = drugType;
    }

    @Column(name = "UseDose", precision = 10, scale = 3)
    public Double getUseDose() {
        return useDose;
    }

    public void setUseDose(Double useDose) {
        this.useDose = useDose;
    }

    @Column(name = "UseDoseUnit", length = 6)
    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    @Column(name = "UsingRate", length = 10)
    public String getUsingRate() {
        return usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    @Column(name = "UsePathways", length = 10)
    public String getUsePathways() {
        return usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    @Column(name = "Producer", length = 20)
    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    @Column(name = "Price", precision = 10)
    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    @Column(name = "SourceOrgan", length = 11)
    public Integer getSourceOrgan() {
        return sourceOrgan;
    }

    public void setSourceOrgan(Integer sourceOrgan) {
        this.sourceOrgan = sourceOrgan;
    }

    @Column(name = "CreateDt", length = 19)
    public Date getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
    }

    @Column(name = "LastModify", length = 19)
    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    @Column(name = "approvalNumber", length = 100)
    public String getApprovalNumber() {
        return approvalNumber;
    }

    public void setApprovalNumber(String approvalNumber) {
        this.approvalNumber = approvalNumber;
    }

    @Column(name = "standardCode", length = 30)
    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
    }

    @Column(name = "drugForm", length = 20)
    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }

    @Column(name = "Status", length = 11)
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "RejectReason", length = 100)
    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    @Column(name = "Type", length = 11)
    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    @Column(name = "SourceEnterprise", length = 50)
    public String getSourceEnterprise() {
        return sourceEnterprise;
    }

    public void setSourceEnterprise(String sourceEnterprise) {
        this.sourceEnterprise = sourceEnterprise;
    }

    @Column(name = "DrugId", length = 11)
    public Integer getDrugId() {
        return drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }
}
