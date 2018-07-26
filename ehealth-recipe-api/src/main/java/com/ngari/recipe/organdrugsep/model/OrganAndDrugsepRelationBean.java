package com.ngari.recipe.organdrugsep.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 组织与药企间关系
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/6.
 */
@Schema
public class OrganAndDrugsepRelationBean implements java.io.Serializable {

    private static final long serialVersionUID = 2439194296959727414L;

    @ItemProperty(alias = "药企序号")
    private Integer id;

    @ItemProperty(alias = "组织序号")
    private Integer organId;

    @ItemProperty(alias = "药企序号")
    private Integer drugsEnterpriseId;

    public OrganAndDrugsepRelationBean() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public Integer getDrugsEnterpriseId() {
        return drugsEnterpriseId;
    }

    public void setDrugsEnterpriseId(Integer drugsEnterpriseId) {
        this.drugsEnterpriseId = drugsEnterpriseId;
    }
}
