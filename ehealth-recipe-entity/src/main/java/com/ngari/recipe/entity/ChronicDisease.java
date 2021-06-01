package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by Erek on 2020/4/20.
 */
@Schema
@Entity
@Table(name = "base_chronicdisease")
@Access(AccessType.PROPERTY)
public class ChronicDisease implements Serializable {
    private static final long serialVersionUID = -529657298227985017L;

    @ItemProperty(alias="自增id")
    private Integer chronicDiseaseId;
    @ItemProperty(alias="病种代码")
    private String chronicDiseaseCode;
    @ItemProperty(alias="病种名称")
    private String chronicDiseaseName;
    @ItemProperty(alias="病种操作码")
    private String chronicDiseaseOptionCode;
    @ItemProperty(alias="'病种类型 1 无  2 特慢病病种 3重症病种 4 慢病病种'")
    private String chronicDiseaseFlag;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "chronicDiseaseId", unique = true, nullable = false)
    public Integer getChronicDiseaseId() {
        return chronicDiseaseId;
    }

    public void setChronicDiseaseId(Integer chronicDiseaseId) {
        this.chronicDiseaseId = chronicDiseaseId;
    }

    @Column(name = "chronicDiseaseCode")
    public String getChronicDiseaseCode() {
        return chronicDiseaseCode;
    }

    public void setChronicDiseaseCode(String chronicDiseaseCode) {
        this.chronicDiseaseCode = chronicDiseaseCode;
    }

    @Column(name = "chronicDiseaseName")
    public String getChronicDiseaseName() {
        return chronicDiseaseName;
    }

    public void setChronicDiseaseName(String chronicDiseaseName) {
        this.chronicDiseaseName = chronicDiseaseName;
    }

    @Column(name = "chronicDiseaseOptionCode")
    public String getChronicDiseaseOptionCode() {
        return chronicDiseaseOptionCode;
    }

    public void setChronicDiseaseOptionCode(String chronicDiseaseOptionCode) {
        this.chronicDiseaseOptionCode = chronicDiseaseOptionCode;
    }

    @Column(name = "chronicDiseaseFlag")
    public String getChronicDiseaseFlag() {
        return chronicDiseaseFlag;
    }

    public void setChronicDiseaseFlag(String chronicDiseaseFlag) {
        this.chronicDiseaseFlag = chronicDiseaseFlag;
    }
}
