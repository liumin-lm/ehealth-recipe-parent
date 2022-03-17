package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.Date;

/**
 * @author rfh
 */
@Schema
public class TcmTreatmentDTO implements Serializable {

    private static final long serialVersionUID = 8617055210574426569L;

    @ItemProperty(alias = "治法ID")
    private Integer id;

    @ItemProperty(alias = "治法编码")
    private String treatmentCode;

    @ItemProperty(alias = "治法名称")
    private String treatmentName;


    @ItemProperty(alias = "机构ID")
    private Integer organId;

    @ItemProperty(alias = "创建时间")
    private Date createDate;

    @ItemProperty(alias = "修改时间")
    private Date modifyDate;

    @ItemProperty(alias = "关联监管治法编码")
    private String regulationTreatmentCode;

    @ItemProperty(alias = "关联监管治法名称")
    private String regulationTreatmentName;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTreatmentCode() {
        return treatmentCode;
    }

    public void setTreatmentCode(String treatmentCode) {
        this.treatmentCode = treatmentCode;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public void setTreatmentName(String treatmentName) {
        this.treatmentName = treatmentName;
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

    public String getRegulationTreatmentCode() {
        return regulationTreatmentCode;
    }

    public void setRegulationTreatmentCode(String regulationTreatmentCode) {
        this.regulationTreatmentCode = regulationTreatmentCode;
    }

    public String getRegulationTreatmentName() {
        return regulationTreatmentName;
    }

    public void setRegulationTreatmentName(String regulationTreatmentName) {
        this.regulationTreatmentName = regulationTreatmentName;
    }
}
