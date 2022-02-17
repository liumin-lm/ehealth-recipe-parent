package com.ngari.recipe.drug.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * cdr_decoctionWay中药煎法表
 */
@Schema
public class DecoctionWayBean implements Serializable {

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
        alias = "煎法排序"
    )
    private Integer sort;

    @ItemProperty(
            alias = "是否代煎"
    )

    private Boolean generationisOfDecoction;
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

    public Boolean getGenerationisOfDecoction() {
        return generationisOfDecoction;
    }

    public void setGenerationisOfDecoction(Boolean generationisOfDecoction) {
        this.generationisOfDecoction = generationisOfDecoction;
    }
}
