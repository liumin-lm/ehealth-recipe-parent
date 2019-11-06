package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * created by shiyuping on 2019/10/25
 * @author shiyuping
 */
@Entity
@Schema
@Table(name = "cdr_organ_medicationguide_relation")
@Access(AccessType.PROPERTY)
public class OrganMedicationGuideRelation implements Serializable {
    private static final long serialVersionUID = -951101023300220108L;

    @ItemProperty(alias = "序号")
    private Integer organGuideRelationId;

    @ItemProperty(alias = "组织ID")
    private Integer organId;

    @ItemProperty(alias = "审方机构ID")
    private Integer guideId;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "organGuideRelationId", unique = true, nullable = false)
    public Integer getOrganGuideRelationId() {
        return organGuideRelationId;
    }

    public void setOrganGuideRelationId(Integer organGuideRelationId) {
        this.organGuideRelationId = organGuideRelationId;
    }

    @Column(name = "organId")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "guideId")
    public Integer getGuideId() {
        return guideId;
    }

    public void setGuideId(Integer guideId) {
        this.guideId = guideId;
    }
}
