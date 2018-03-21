package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by  on 2016/10/25 0025.
 * 处方审核记录详情
 * @author zhongzx
 */

@Schema
@Entity
@Table(name="cdr_recipecheckdetail")
@Access(AccessType.PROPERTY)
public class RecipeCheckDetail implements Serializable{

    private static final long serialVersionUID = -8083582758617582290L;

    @ItemProperty(alias = "审核详情序号")
    private Integer checkDetailId;

    @ItemProperty(alias = "审核序号")
    private Integer checkId;

    @ItemProperty(alias = "处方详情序号")
    private String recipeDetailIds;

    @ItemProperty(alias = "原因序号")
    @Dictionary(id = "eh.cdr.dictionary.Reason")
    private String reasonIds;

    @Id
    @GeneratedValue(strategy=IDENTITY)
    @Column(name = "CheckDetailId", unique = true, nullable = false)
    public Integer getCheckDetailId() {
        return checkDetailId;
    }

    public void setCheckDetailId(Integer checkDetailId) {
        this.checkDetailId = checkDetailId;
    }

    @Column(name = "CheckId", nullable = false)
    public Integer getCheckId() {
        return checkId;
    }

    public void setCheckId(Integer checkId) {
        this.checkId = checkId;
    }

    @Column(name = "RecipeDetailIds")
    public String getRecipeDetailIds() {
        return recipeDetailIds;
    }

    public void setRecipeDetailIds(String recipeDetailIds) {
        this.recipeDetailIds = recipeDetailIds;
    }

    @Column(name = "ReasonIds")
    public String getReasonIds() {
        return reasonIds;
    }

    public void setReasonIds(String reasonIds) {
        this.reasonIds = reasonIds;
    }
}
