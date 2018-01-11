package recipe.bean;

import com.ngari.recipe.entity.Recipe;

/**
 * @author： 0184/yu_yun
 * @date： 2017/12/11
 * @description： 第三方交互结果对象
 * @version： 1.0
 */
public class ThirdResultBean extends RecipeResultBean{

    private Recipe recipe;

    private String recipeCode;

    public ThirdResultBean(Integer code) {
        setCode(code);
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public static ThirdResultBean getSuccess() {
        return new ThirdResultBean(SUCCESS);
    }

    public static ThirdResultBean getFail() {
        return new ThirdResultBean(FAIL);
    }
}
