package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by  on 2017/1/1.
 * @author Chuwei
 */
@Schema
@Entity
@Table(name = "cdr_dispensatory")
@Access(AccessType.PROPERTY)
public class Dispensatory implements java.io.Serializable{

    private static final long serialVersionUID = -9091300924705911507L;
    @ItemProperty(alias="药品说明书序号")
    private Integer dispensatoryId;

    @ItemProperty(alias="药品名称")
    private String name;

    @ItemProperty(alias="生产厂家")
    private String manufacturers;

    @ItemProperty(alias="通用名")
    private String drugName;

    @ItemProperty(alias="英文名")
    private String englishName;

    @ItemProperty(alias="商品名")
    private String saleName;

    @ItemProperty(alias="规格")
    private String specs;

    @ItemProperty(alias="成分")
    private String composition;

    @ItemProperty(alias = "性状")
    private String property;

    @ItemProperty(alias="适应症")
    private String indication;

    @ItemProperty(alias="用法用量")
    private String dosage;

    @ItemProperty(alias="不良反应")
    private String reactions;

    @ItemProperty(alias="禁忌")
    private String contraindications;

    @ItemProperty(alias="注意事项")
    private String attention;

    @ItemProperty(alias="特殊人群用药")
    private String specialPopulations;

    @ItemProperty(alias="药物相互作用")
    private String interaction;

    @ItemProperty(alias="药理作用")
    private String pharmacological;

    @ItemProperty(alias="贮藏")
    private String storage;

    @ItemProperty(alias="有效期")
    private String validityDate;

    @ItemProperty(alias="批准文号")
    private String approvalNumber;

    @ItemProperty(alias="页面ID")
    private String pageId;

    @ItemProperty(alias="图片地址")
    private String picUrl;

    @ItemProperty(alias = "信息来源")
    private Integer source;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "最后操作时间")
    private Date lastModifyTime;

    @ItemProperty(alias = "药品Id")
    private Integer drugId;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name="dispensatoryId", unique = true, nullable = false, length = 11)
    public Integer getDispensatoryId() {
        return dispensatoryId;
    }
    public void setDispensatoryId(Integer dispensatoryId) {
        this.dispensatoryId = dispensatoryId;
    }

    @Column(name="name", nullable = false)
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @Column (name="manufacturers")
    public String getManufacturers() {
        return manufacturers;
    }
    public void setManufacturers(String manufacturers) {
        this.manufacturers = manufacturers;
    }

    @Column (name="drugName")
    public String getDrugName() {
        return drugName;
    }
    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    @Column (name="EnglishName")
    public String getEnglishName() {
        return englishName;
    }
    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    @Column (name="SaleName")
    public String getSaleName() {
        return saleName;
    }
    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    @Column (name="specs")
    public String getSpecs() {
        return specs;
    }
    public void setSpecs(String specs) {
        this.specs = specs;
    }

    @Column (name="composition")
    public String getComposition() {
        return composition;
    }
    public void setComposition(String composition) {
        this.composition = composition;
    }

    @Column (name="property")
    public String getProperty(){ return property; }
    public void setProperty(String property) { this.property = property; }

    @Column (name="indication")
    public String getIndication() {
        return indication;
    }
    public void setIndication(String indication) {
        this.indication = indication;
    }

    @Column (name="dosage")
    public String getDosage() {
        return dosage;
    }
    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    @Column (name="reactions")
    public String getReactions() {
        return reactions;
    }
    public void setReactions(String reactions) {
        this.reactions = reactions;
    }

    @Column (name="contraindications")
    public String getContraindications() {
        return contraindications;
    }
    public void setContraindications(String contraindications) {
        this.contraindications = contraindications;
    }

    @Column (name="attention")
    public String getAttention() {
        return attention;
    }
    public void setAttention(String attention) {
        this.attention = attention;
    }

    @Column (name="specialPopulations")
    public void setSpecialPopulations(String specialPopulations) {
        this.specialPopulations = specialPopulations;
    }
    public String getSpecialPopulations() {
        return specialPopulations;
    }

    @Column (name="interaction")
    public void setInteraction(String interaction) {
        this.interaction = interaction;
    }
    public String getInteraction() {
        return interaction;
    }

    @Column (name="pharmacological")
    public void setPharmacological(String pharmacological) {
        this.pharmacological = pharmacological;
    }
    public String getPharmacological() {
        return pharmacological;
    }

    @Column (name="validityDate")
    public void setValidityDate(String validityDate) {
        this.validityDate = validityDate;
    }
    public String getValidityDate() {
        return validityDate;
    }

    @Column (name="storage")
    public void setStorage(String storage) {
        this.storage = storage;
    }
    public String getStorage() {
        return storage;
    }

    @Column (name="approvalNumber")
    public void setApprovalNumber(String approvalNumber) {
        this.approvalNumber = approvalNumber;
    }
    public String getApprovalNumber() { return approvalNumber; }

    @Column (name="pageId")
    public String getPageId() {
        return pageId;
    }
    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    @Column (name="picUrl")
    public String getPicUrl() {
        return picUrl;
    }
    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    @Column (name="source")
    public Integer getSource() {
        return source;
    }
    public void setSource(Integer source) {
        this.source = source;
    }

    @Column (name="createTime")
    public Date getCreateTime() {
        return createTime;
    }
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column (name="lastModifyTime")
    public Date getLastModifyTime() {
        return lastModifyTime;
    }
    public void setLastModifyTime(Date lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    @Column (name="drugId")
    public Integer getDrugId(){ return drugId; }
    public void setDrugId(Integer drugId){ this.drugId = drugId; }

}
