package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author yinsheng
 * @date 2019\10\9 0009 11:02
 */
@Entity
@Schema
@Table(name = "cdr_organ_judicial_relation")
@Access(AccessType.PROPERTY)
public class OrganJudicialRelation implements Serializable{
    private static final long serialVersionUID = 5700321527689204229L;

    @ItemProperty(alias = "序号")
    private Integer organJudRelationId;

    @ItemProperty(alias = "组织ID")
    private Integer organId;

    @ItemProperty(alias = "审方机构ID")
    private Integer judicialorganId;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "OrganJudRelationId", unique = true, nullable = false)
    public Integer getOrganJudRelationId() {
        return organJudRelationId;
    }

    public void setOrganJudRelationId(Integer organJudRelationId) {
        this.organJudRelationId = organJudRelationId;
    }

    @Column(name = "OrganId")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "JudicialorganId")
    public Integer getJudicialorganId() {
        return judicialorganId;
    }

    public void setJudicialorganId(Integer judicialorganId) {
        this.judicialorganId = judicialorganId;
    }
}
