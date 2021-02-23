package com.ngari.recipe.commonrecipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by  on 2017/5/22.
 *
 * @author jiangtingfeng
 */
@Schema
public class CommonRecipeDTO implements Serializable {

    private static final long serialVersionUID = -1946631470972113416L;

    @ItemProperty(alias = "医生身份ID")
    private Integer doctorId;

    @ItemProperty(alias = "常用方名称")
    private String commonRecipeName;

    @ItemProperty(alias = "常用方Id")
    private Integer commonRecipeId;

    @ItemProperty(alias = "处方类型")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "机构代码")
    private Integer organId;

    //开当前处方的配置项信息
    @ItemProperty(alias = "可开长处方按钮状态、长处方开药天数、非长处方开药天数")
    private String recipeJsonConfig;

    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;
    @ItemProperty(alias = "药房名称")
    private String pharmacyName;

    @ItemProperty(alias = "失效药品id列表")
    private List<Integer> drugIdList;

    @ItemProperty(alias = "药品列表")
    private List<CommonRecipeDrugDTO> commonDrugList;

    @ItemProperty(alias = "常用方的状态")
    private Integer commonRecipeStatus;

    @ItemProperty(alias = "常用方中的药品信息")
    private String recipeDetailJsonConfig;

    @ItemProperty(alias = "是否是长处方")
    private String isLongRecipe;

    @ItemProperty(alias = "药房编码")
    private String pharmacyCode;
    @ItemProperty(alias = "常用方扩展信息")
    private CommonRecipeExtDTO commonRecipeExt;

    public String getPharmacyCode() {
        return pharmacyCode;
    }

    public void setPharmacyCode(String pharmacyCode) {
        this.pharmacyCode = pharmacyCode;
    }

    public String getRecipeDetailJsonConfig() {
        return recipeDetailJsonConfig;
    }

    public void setRecipeDetailJsonConfig(String recipeDetailJsonConfig) {
        this.recipeDetailJsonConfig = recipeDetailJsonConfig;
    }

    public String getIsLongRecipe() {
        return isLongRecipe;
    }

    public void setIsLongRecipe(String isLongRecipe) {
        this.isLongRecipe = isLongRecipe;
    }

    public CommonRecipeDTO() {
    }

    public Integer getCommonRecipeStatus() {
        return commonRecipeStatus;
    }

    public void setCommonRecipeStatus(Integer commonRecipeStatus) {
        this.commonRecipeStatus = commonRecipeStatus;
    }

    public Integer getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(Integer pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    public List<Integer> getDrugIdList() {
        return drugIdList;
    }

    public void setDrugIdList(List<Integer> drugIdList) {
        this.drugIdList = drugIdList;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public String getCommonRecipeName() {
        return commonRecipeName;
    }

    public void setCommonRecipeName(String commonRecipeName) {
        this.commonRecipeName = commonRecipeName;
    }

    public Integer getCommonRecipeId() {
        return commonRecipeId;
    }

    public void setCommonRecipeId(Integer commonRecipeId) {
        this.commonRecipeId = commonRecipeId;
    }

    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }

    public Date getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getRecipeJsonConfig() {
        return recipeJsonConfig;
    }

    public void setRecipeJsonConfig(String recipeJsonConfig) {
        this.recipeJsonConfig = recipeJsonConfig;
    }

    public List<CommonRecipeDrugDTO> getCommonDrugList() {
        return commonDrugList;
    }

    public void setCommonDrugList(List<CommonRecipeDrugDTO> commonDrugList) {
        this.commonDrugList = commonDrugList;
    }

    public CommonRecipeExtDTO getCommonRecipeExt() {
        return commonRecipeExt;
    }

    public void setCommonRecipeExt(CommonRecipeExtDTO commonRecipeExt) {
        this.commonRecipeExt = commonRecipeExt;
    }

    @Override
    public String toString() {
        return "CommonRecipeDTO{" +
                "doctorId=" + doctorId +
                ", commonRecipeName='" + commonRecipeName + '\'' +
                ", commonRecipeId=" + commonRecipeId +
                ", recipeType=" + recipeType +
                ", createDt=" + createDt +
                ", lastModify=" + lastModify +
                ", organId=" + organId +
                ", recipeJsonConfig='" + recipeJsonConfig + '\'' +
                ", pharmacyId=" + pharmacyId +
                ", pharmacyName='" + pharmacyName + '\'' +
                ", drugIdList=" + drugIdList +
                ", commonRecipeStatus=" + commonRecipeStatus +
                ", recipeDetailJsonConfig='" + recipeDetailJsonConfig + '\'' +
                ", isLongRecipe='" + isLongRecipe + '\'' +
                ", pharmacyCode='" + pharmacyCode + '\'' +
                '}';
    }
}
