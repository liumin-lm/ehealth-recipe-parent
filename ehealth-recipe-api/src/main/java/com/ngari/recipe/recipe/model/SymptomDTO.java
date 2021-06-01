package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

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
}
