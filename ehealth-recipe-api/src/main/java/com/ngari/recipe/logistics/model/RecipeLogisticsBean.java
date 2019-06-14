package com.ngari.recipe.logistics.model;

/**
 * @author yinsheng
 * @date 2019\6\3 0003 15:59
 */
public class RecipeLogisticsBean implements java.io.Serializable {
    private static final long serialVersionUID = -2635972690705891360L;

    private String recipeId;
    private String recipeCode;
    private String organId;
    private String clinicOrgan;
    private String account;
    private Logistics logistics;

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    public String getClinicOrgan() {
        return clinicOrgan;
    }

    public void setClinicOrgan(String clinicOrgan) {
        this.clinicOrgan = clinicOrgan;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Logistics getLogistics() {
        return logistics;
    }

    public void setLogistics(Logistics logistics) {
        this.logistics = logistics;
    }
}
