package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 中医症候
 * @author renfuhao
 */
@Entity
@Schema
@Table(name = "recipe_symptom")
@Access(AccessType.PROPERTY)
public class Symptom implements java.io.Serializable{

    private static final long serialVersionUID = -5561405173590960663L;

    @ItemProperty(alias = "症候ID")
    private Integer symptomId;

    @ItemProperty(alias = "症候编码")
    private String symptomCode;

    @ItemProperty(alias = "症候名称")
    private String symptomName;

    @ItemProperty(alias = "症候拼音")
    private String pinYin;

    @ItemProperty(alias = "机构ID")
    private Integer organId;

    @ItemProperty(alias = "创建时间")
    private Date createDate;

    @ItemProperty(alias = "修改时间")
    private Date modifyDate;

    @ItemProperty(alias = "关联治法编码")
    private String treatmentCode;

    @ItemProperty(alias = "关联治法名称")
    private String treatmentName;

    @ItemProperty(alias = "关联监管证候编码")
    private String regulationSymptomCode;

    @ItemProperty(alias = "关联监管证候名称")
    private String regulationSymptomName;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "symptomId", unique = true, nullable = false)
    public Integer getSymptomId() {
        return symptomId;
    }

    public void setSymptomId(Integer symptomId) {
        this.symptomId = symptomId;
    }


    @Column(name = "symptomCode")
    public String getSymptomCode() {
        return symptomCode;
    }

    public void setSymptomCode(String symptomCode) {
        this.symptomCode = symptomCode;
    }


    @Column(name = "symptomName")
    public String getSymptomName() {
        return symptomName;
    }

    public void setSymptomName(String symptomName) {
        this.symptomName = symptomName;
    }

    @Column(name = "pinYin")
    public String getPinYin() {
        return pinYin;
    }

    public void setPinYin(String pinYin) {
        this.pinYin = pinYin;
    }

    @Column(name = "organId")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "createDate")
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column(name = "modifyDate")
    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    @Column(name = "treatment_code")
    public String getTreatmentCode() {
        return treatmentCode;
    }

    public void setTreatmentCode(String treatmentCode) {
        this.treatmentCode = treatmentCode;
    }

    @Column(name = "treatment_name")
    public String getTreatmentName() {
        return treatmentName;
    }

    public void setTreatmentName(String treatmentName) {
        this.treatmentName = treatmentName;
    }

    @Column(name = "regulation_symptom_code")
    public String getRegulationSymptomCode() {
        return regulationSymptomCode;
    }

    public void setRegulationSymptomCode(String regulationSymptomCode) {
        this.regulationSymptomCode = regulationSymptomCode;
    }

    @Column(name = "regulation_symptom_name")
    public String getRegulationSymptomName() {
        return regulationSymptomName;
    }

    public void setRegulationSymptomName(String regulationSymptomName) {
        this.regulationSymptomName = regulationSymptomName;
    }
}
