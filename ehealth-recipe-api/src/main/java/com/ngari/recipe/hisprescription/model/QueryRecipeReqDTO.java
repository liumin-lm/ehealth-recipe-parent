package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;

/**
 * 浙江互联网医院查询处方接口请求dto
 * created by shiyuping on 2018/11/30
 */
public class QueryRecipeReqDTO implements Serializable {

    private static final long serialVersionUID = 1836794424212428410L;

    private String organId;//组织机构编码
    private String clinicID;//复诊ID
    private String recipeID;//平台处方号
    private String platRecipeId;//平台处方id

    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    public String getClinicID() {
        return clinicID;
    }

    public void setClinicID(String clinicID) {
        this.clinicID = clinicID;
    }

    public String getRecipeID() {
        return recipeID;
    }

    public void setRecipeID(String recipeID) {
        this.recipeID = recipeID;
    }

    public String getPlatRecipeId() {
        return platRecipeId;
    }

    public void setPlatRecipeId(String platRecipeId) {
        this.platRecipeId = platRecipeId;
    }
}
