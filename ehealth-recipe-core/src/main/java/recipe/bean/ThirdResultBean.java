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

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public ThirdResultBean(Integer code) {
        setCode(code);
    }

    public static ThirdResultBean getSuccess() {
        return new ThirdResultBean(SUCCESS);
    }

    public static ThirdResultBean getFail() {
        return new ThirdResultBean(FAIL);
    }
}
