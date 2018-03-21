package recipe.prescription.bean;

import com.ngari.recipe.entity.Recipe;
import ctd.util.JSONUtils;
import recipe.common.response.CommonResponse;

import java.io.Serializable;

/**
 * 对接医院HIS结果对象
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2017/4/18.
 */
public class HosRecipeResult extends CommonResponse implements Serializable {

    private static final long serialVersionUID = 2809725502013933071L;

    private String recipeCode;

    private Integer recipeId;

    private Recipe recipe;

    public HosRecipeResult() {

    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public String toString() {
        return JSONUtils.toString(this);
    }

}
