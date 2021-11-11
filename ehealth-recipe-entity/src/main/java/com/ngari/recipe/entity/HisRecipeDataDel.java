package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author zgy
 * @date 2021\11\10
 */
@Entity
@Schema
@Table(name = "cdr_his_recipe_data_del")
@Access(AccessType.PROPERTY)
public class HisRecipeDataDel implements Serializable {

    private static final long serialVersionUID = 2631096495864599880L;

    @ItemProperty(alias = "序号")
    private Integer id;

    @ItemProperty(alias = "线上处方号")
    private Integer recipeId;

    @ItemProperty(alias = "线下处方号")
    private Integer hisRecipeId;

    @ItemProperty(alias = "处方号码")
    private String recipeCode;

    @ItemProperty(alias = "删除数据的表名")
    private String tableName;

    @ItemProperty(alias = "删除的数据")
    private String data;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "recipe_id")
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Column(name = "his_recipe_id")
    public Integer getHisRecipeId() {
        return hisRecipeId;
    }

    public void setHisRecipeId(Integer hisRecipeId) {
        this.hisRecipeId = hisRecipeId;
    }

    @Column(name = "recipe_code")
    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    @Column(name = "table_name")
    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Column(name = "data")
    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Column(name = "create_time")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
