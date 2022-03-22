package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.beans.Transient;
import java.io.Serializable;
import java.util.Date;

/**
 * @author rfh
 */
@Schema
public class SymptomDTO implements Serializable {


    private static final long serialVersionUID = 6178954018845379277L;


    private Integer symptomId;

    private String symptomCode;

    private String symptomName;

    private String pinYin;

    private Integer organId;

    private Date createDate;

    private Date modifyDate;


    @ItemProperty(alias = "关联治法编码")
    private String treatmentCode;

    @ItemProperty(alias = "关联监管证候编码")
    private String regulationSymptomCode;

    @ItemProperty(alias = "关联监管证候名称")
    private String regulationSymptomName;


    @ItemProperty(alias = "关联治法名称")
    private String treatmentName;


    public Integer getSymptomId() {
        return symptomId;
    }

    public void setSymptomId(Integer symptomId) {
        this.symptomId = symptomId;
    }

    public String getSymptomCode() {
        return symptomCode;
    }

    public void setSymptomCode(String symptomCode) {
        this.symptomCode = symptomCode;
    }

    public String getSymptomName() {
        return symptomName;
    }

    public void setSymptomName(String symptomName) {
        this.symptomName = symptomName;
    }

    public String getPinYin() {
        return pinYin;
    }

    public void setPinYin(String pinYin) {
        this.pinYin = pinYin;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    public String getTreatmentCode() {
        return treatmentCode;
    }

    public void setTreatmentCode(String treatmentCode) {
        this.treatmentCode = treatmentCode;
    }

    public String getRegulationSymptomCode() {
        return regulationSymptomCode;
    }

    public void setRegulationSymptomCode(String regulationSymptomCode) {
        this.regulationSymptomCode = regulationSymptomCode;
    }

    public String getRegulationSymptomName() {
        return regulationSymptomName;
    }

    public void setRegulationSymptomName(String regulationSymptomName) {
        this.regulationSymptomName = regulationSymptomName;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public void setTreatmentName(String treatmentName) {
        this.treatmentName = treatmentName;
    }
}
