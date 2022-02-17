package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 组织与药企间关系
 *
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/6.
 */

@Entity
@Schema
@Table(name = "cdr_organ_drugsep_relation")
@Access(AccessType.PROPERTY)
public class OrganAndDrugsepRelation implements java.io.Serializable {

    private static final long serialVersionUID = 2439194296959727414L;

    @ItemProperty(alias = "药企序号")
    private Integer id;

    @ItemProperty(alias = "组织序号")
    private Integer organId;

    @ItemProperty(alias = "药企序号")
    private Integer drugsEnterpriseId;

    @ItemProperty(alias = "药企支持的购药方式")
    private String drugsEnterpriseSupportGiveMode;


    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "OrganId")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "DrugsEnterpriseId")
    public Integer getDrugsEnterpriseId() {
        return drugsEnterpriseId;
    }

    public void setDrugsEnterpriseId(Integer drugsEnterpriseId) {
        this.drugsEnterpriseId = drugsEnterpriseId;
    }

    @Column(name = "drug_enterprise_support_give_mode")
    public String getDrugsEnterpriseSupportGiveMode() {
        return drugsEnterpriseSupportGiveMode;
    }

    public void setDrugsEnterpriseSupportGiveMode(String drugsEnterpriseSupportGiveMode) {
        this.drugsEnterpriseSupportGiveMode = drugsEnterpriseSupportGiveMode;
    }
}
