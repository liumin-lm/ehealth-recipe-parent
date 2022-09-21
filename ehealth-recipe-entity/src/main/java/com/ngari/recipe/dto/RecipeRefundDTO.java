package com.ngari.recipe.dto;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Schema
public class RecipeRefundDTO implements Serializable {
    private static final long serialVersionUID = 539370524183685327L;

    @ItemProperty(alias = "处方单号")
    private Integer recipeId;

    @ItemProperty(alias = "患者姓名")
    private String patientName;

    @ItemProperty(alias = "处方类型 1 西药 2 中成药 3 中药 4膏方")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @ItemProperty(alias = "取消原因")
    private String reason;

    @ItemProperty(alias = "开方时间")
    private Date createDate;


    @Id
    @GeneratedValue(strategy = IDENTITY)
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}
