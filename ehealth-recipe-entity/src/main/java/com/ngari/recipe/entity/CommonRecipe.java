package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by  on 2017/5/22.
 * @author jiangtingfeng
 */
@Schema
@Entity
@Table(name = "cdr_commonRecipe")
@Access(AccessType.PROPERTY)
public class CommonRecipe implements Serializable{

    private static final long serialVersionUID = 1500970890296225446L;

    @ItemProperty(alias = "医生身份ID")
    private Integer doctorId;

    @ItemProperty(alias = "常用方名称")
    private String commonRecipeName;

    @ItemProperty(alias = "常用方Id")
    private Integer commonRecipeId;

    @ItemProperty(alias = "常用方编码-医院唯一主键字段")
    private String commonRecipeCode;

    @ItemProperty(alias = "常用方类型：1平台，2协定方，3....")
    private Integer commonRecipeType;

    @ItemProperty(alias = "处方类型")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;
//
//    @ItemProperty(alias = "创建时间")
//    private Date createDt;
//
//    @ItemProperty(alias = "最后修改时间")
//    private Date lastModify;

    @ItemProperty(alias = "机构代码")
    private Integer organId;

    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;
    @ItemProperty(alias = "药房名称")
    private String pharmacyName;
    @ItemProperty(alias = "药房编码")
    private String pharmacyCode;

    @ItemProperty(alias = "是否是长处方")
    private String isLongRecipe;


    //开当前处方的配置项信息
    @ItemProperty(alias = "可开长处方按钮状态、长处方开药天数、非长处方开药天数")
    private String recipeJsonConfig;


    @Column(name = "pharmacyCode")
    public String getPharmacyCode() {
        return pharmacyCode;
    }

    public void setPharmacyCode(String pharmacyCode) {
        this.pharmacyCode = pharmacyCode;
    }

    @Column(name = "isLongRecipe")
    public String getIsLongRecipe() {
        return isLongRecipe;
    }

    public void setIsLongRecipe(String isLongRecipe) {
        this.isLongRecipe = isLongRecipe;
    }

    @Column(name = "OrganId")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "CommonRecipeId", nullable = false)
    public Integer getCommonRecipeId() {
        return commonRecipeId;
    }

    public void setCommonRecipeId(Integer commonRecipeId) {
        this.commonRecipeId = commonRecipeId;
    }

    @Column(name = "RecipeType", nullable = false)
    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }

    @Column(name = "DoctorId", nullable = false)
    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    @Column(name = "CommonRecipeName", nullable = false)
    public String getCommonRecipeName() {
        return commonRecipeName;
    }

    public void setCommonRecipeName(String commonRecipeName) {
        this.commonRecipeName = commonRecipeName;
    }
//
//    @Column(name = "CreateDt", length = 19)
//    public Date getCreateDt() {
//        return createDt;
//    }
//
//    public void setCreateDt(Date createDt) {
//        this.createDt = createDt;
//    }
//
//    @Column(name = "LastModify", length = 19)
//    public Date getLastModify() {
//        return lastModify;
//    }
//
//    public void setLastModify(Date lastModify) {
//        this.lastModify = lastModify;
//    }

    public String getRecipeJsonConfig() {
        return recipeJsonConfig;
    }

    public void setRecipeJsonConfig(String recipeJsonConfig) {
        this.recipeJsonConfig = recipeJsonConfig;
    }

    @Column(name = "pharmacyId")
    public Integer getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(Integer pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    @Column(name = "pharmacyName")
    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    @Column(name = "common_recipe_code")
    public String getCommonRecipeCode() {
        return commonRecipeCode;
    }

    public void setCommonRecipeCode(String commonRecipeCode) {
        this.commonRecipeCode = commonRecipeCode;
    }

    @Column(name = "common_recipe_type")
    public Integer getCommonRecipeType() {
        return commonRecipeType;
    }

    public void setCommonRecipeType(Integer commonRecipeType) {
        this.commonRecipeType = commonRecipeType;
    }
}
