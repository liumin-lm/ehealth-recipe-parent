package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 服用要求
 */
@Entity
@Schema
@Table(name = "requirements_for_taking")
@Access(AccessType.PROPERTY)
public class RequirementsForTaking implements Serializable {

    private static final long serialVersionUID = -7396436464542532302L;
    @ItemProperty(
        alias = "id"
    )
    private Integer id;

    @ItemProperty(
        alias = "机构编码"
    )
    private Integer organId;

    @ItemProperty(
        alias = "编码"
    )
    private String code;

    @ItemProperty(
        alias = "名称"
    )
    private String text;

    @ItemProperty(
        alias = "排序"
    )
    private Integer sort;


    @ItemProperty(
            alias = "煎法"
    )
    private String decoctionwayId;


    @Id
    @GeneratedValue(strategy = IDENTITY)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "organ_id")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Column(name = "decoctionway_id")
    public String getDecoctionwayId() {
        return decoctionwayId;
    }

    public void setDecoctionwayId(String decoctionwayId) {
        this.decoctionwayId = decoctionwayId;
    }
}

