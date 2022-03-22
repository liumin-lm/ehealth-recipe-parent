package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 中医治法
 * @author renfuhao
 */
@Entity
@Schema
@Table(name = "recipe_treatment")
@Access(AccessType.PROPERTY)
public class TcmTreatment  implements Serializable {

    private static final long serialVersionUID = 5531180943928895690L;

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

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    @Column(name = "organ_id")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "create_date")
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column(name = "modify_date")
    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    @Column(name = "regulation_treatment_code")
    public String getRegulationTreatmentCode() {
        return regulationTreatmentCode;
    }

    public void setRegulationTreatmentCode(String regulationTreatmentCode) {
        this.regulationTreatmentCode = regulationTreatmentCode;
    }

    @Column(name = "regulation_treatment_name")
    public String getRegulationTreatmentName() {
        return regulationTreatmentName;
    }

    public void setRegulationTreatmentName(String regulationTreatmentName) {
        this.regulationTreatmentName = regulationTreatmentName;
    }
}
