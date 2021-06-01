package com.ngari.recipe.drugsenterprise.model;

import com.ngari.recipe.recipe.model.RecipeDetailBean;

import java.io.Serializable;
import java.util.List;

/**
 * @author yinsheng
 * @date 2020\9\8 0008 17:46
 */
public class DrugsDataBean implements Serializable{
    private static final long serialVersionUID = 3228197873768044898L;

    private Integer organId;
    private List<RecipeDetailBean> recipeDetailBeans;
    private String newVersionFlag;

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public List<RecipeDetailBean> getRecipeDetailBeans() {
        return recipeDetailBeans;
    }

    public void setRecipeDetailBeans(List<RecipeDetailBean> recipeDetailBeans) {
        this.recipeDetailBeans = recipeDetailBeans;
    }

    public String getNewVersionFlag() {
        return newVersionFlag;
    }

    public void setNewVersionFlag(String newVersionFlag) {
        this.newVersionFlag = newVersionFlag;
    }
}
