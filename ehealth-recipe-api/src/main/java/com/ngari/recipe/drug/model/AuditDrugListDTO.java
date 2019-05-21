package com.ngari.recipe.drug.model;

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
@Schema
public class AuditDrugListDTO implements java.io.Serializable {
    private static final long serialVersionUID = 9041477935506136661L;

    @ItemProperty(alias = "审核药品序号")
    private Integer auditDrugId;

    @ItemProperty(alias = "医疗机构编码")
    private String organizeCode;

    @ItemProperty(alias = "平台机构编码")
    private Integer organId;

    @ItemProperty(alias = "医疗结构药品编码")
    private String organDrugCode;

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

    public Integer getAuditDrugId() {
        return auditDrugId;
    }

    public void setAuditDrugId(Integer auditDrugId) {
        this.auditDrugId = auditDrugId;
    }

    public String getOrganizeCode() {
        return organizeCode;
    }

    public void setOrganizeCode(String organizeCode) {
        this.organizeCode = organizeCode;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    public Integer getSaleDrugListId() {
        return saleDrugListId;
    }

    public void setSaleDrugListId(Integer saleDrugListId) {
        this.saleDrugListId = saleDrugListId;
    }

    public Integer getOrganDrugListId() {
        return organDrugListId;
    }

    public void setOrganDrugListId(Integer organDrugListId) {
        this.organDrugListId = organDrugListId;
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getSourceOrgan() {
        return sourceOrgan;
    }

    public void setSourceOrgan(Integer sourceOrgan) {
        this.sourceOrgan = sourceOrgan;
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

    public String getApprovalNumber() {
        return approvalNumber;
    }

    public void setApprovalNumber(String approvalNumber) {
        this.approvalNumber = approvalNumber;
    }

    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
    }

    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getSourceEnterprise() {
        return sourceEnterprise;
    }

    public void setSourceEnterprise(String sourceEnterprise) {
        this.sourceEnterprise = sourceEnterprise;
    }
}
