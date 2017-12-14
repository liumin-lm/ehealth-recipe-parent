package com.ngari.recipe.recipelog.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import ctd.util.JSONUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/4/29.
 */
@Schema
public class RecipeLogBean implements Serializable {

    private static final long serialVersionUID = 1707921836539282782L;

    @ItemProperty(alias = "id")
    private int id;

    @ItemProperty(alias = "处方序号")
    private Integer recipeId;

    @ItemProperty(alias = "修改时间")
    private Date modifyDate;

    @ItemProperty(alias = "前处方状态")
    @Dictionary(id = "eh.cdr.dictionary.RecipeStatus")
    private Integer beforeStatus;

    @ItemProperty(alias = "后处方状态")
    @Dictionary(id = "eh.cdr.dictionary.RecipeStatus")
    private Integer afterStatus;

    @ItemProperty(alias = "备注信息")
    private String memo;

    @ItemProperty(alias = "预留")
    private String expand;

    public RecipeLogBean() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    public Integer getBeforeStatus() {
        return beforeStatus;
    }

    public void setBeforeStatus(Integer beforeStatus) {
        this.beforeStatus = beforeStatus;
    }

    public Integer getAfterStatus() {
        return afterStatus;
    }

    public void setAfterStatus(Integer afterStatus) {
        this.afterStatus = afterStatus;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getExpand() {
        return expand;
    }

    public void setExpand(String expand) {
        this.expand = expand;
    }

    @Override
    public String toString() {
        return JSONUtils.toString(this);
    }
}
