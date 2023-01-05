package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * cdr_decoctionWay中药煎法表
 */
@Entity
@Schema
@Table(name = "base_drug_decoctionWay")
@Access(AccessType.PROPERTY)
public class DecoctionWay implements Serializable {

    private static final long serialVersionUID = -7396436464542532302L;
    @ItemProperty(
        alias = "煎法id"
    )
    private Integer decoctionId;

    @ItemProperty(
        alias = "机构编码"
    )
    private Integer organId;

    @ItemProperty(
        alias = "煎法编码"
    )
    private String decoctionCode;

    @ItemProperty(
        alias = "煎法名称"
    )
    private String decoctionText;

    @ItemProperty(
        alias = "煎法价格"
    )
    private Double decoctionPrice;

    @ItemProperty(
        alias = "煎法拼音"
    )
    private String decoctionPingying;

    @ItemProperty(
            alias = "剂型"
    )
    private String drugFormType;

    @ItemProperty(
        alias = "煎法排序"
    )
    private Integer sort;

    @ItemProperty(
            alias = "是否代煎"
    )
    private Boolean generationisOfDecoction;

    @ItemProperty(
            alias = "代煎前端展示 0 不展示 1 展示"
    )
    private Integer decoctionExhibitionFlag;

    @ItemProperty(
            alias = "用药途径"
    )
    private String drugUsePathway;

    @ItemProperty(
            alias = "用药频次"
    )
    private String drugUseRate;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    public Integer getDecoctionId() {
        return decoctionId;
    }

    public void setDecoctionId(Integer decoctionId) {
        this.decoctionId = decoctionId;
    }

    public String getDecoctionCode() {
        return decoctionCode;
    }

    public void setDecoctionCode(String decoctionCode) {
        this.decoctionCode = decoctionCode;
    }

    public String getDecoctionText() {
        return decoctionText;
    }

    public void setDecoctionText(String decoctionText) {
        this.decoctionText = decoctionText;
    }

    public Double getDecoctionPrice() {
        return decoctionPrice;
    }

    public void setDecoctionPrice(Double decoctionPrice) {
        this.decoctionPrice = decoctionPrice;
    }

    public String getDecoctionPingying() {
        return decoctionPingying;
    }

    public void setDecoctionPingying(String decoctionPingying) {
        this.decoctionPingying = decoctionPingying;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "generationis_of_decoction")
    public Boolean getGenerationisOfDecoction() {
        return generationisOfDecoction;
    }

    public void setGenerationisOfDecoction(Boolean generationisOfDecoction) {
        this.generationisOfDecoction = generationisOfDecoction;
    }

    @Column(name = "drug_use_pathway")
    public String getDrugUsePathway() {
        return drugUsePathway;
    }

    public void setDrugUsePathway(String drugUsePathway) {
        this.drugUsePathway = drugUsePathway;
    }

    @Column(name = "drug_use_rate")
    public String getDrugUseRate() {
        return drugUseRate;
    }

    public void setDrugUseRate(String drugUseRate) {
        this.drugUseRate = drugUseRate;
    }

    @Column(name = "drug_form_type")
    public String getDrugFormType() {
        return drugFormType;
    }

    public void setDrugFormType(String drugFormType) {
        this.drugFormType = drugFormType;
    }

    @Column(name = "decoction_exhibition_flag")
    public Integer getDecoctionExhibitionFlag() {
        return decoctionExhibitionFlag;
    }

    public void setDecoctionExhibitionFlag(Integer decoctionExhibitionFlag) {
        this.decoctionExhibitionFlag = decoctionExhibitionFlag;
    }
}
