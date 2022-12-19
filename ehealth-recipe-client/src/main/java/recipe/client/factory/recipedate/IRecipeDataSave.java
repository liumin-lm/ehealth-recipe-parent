package recipe.client.factory.recipedate;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;

/**
 * @author fuzi
 */
public interface IRecipeDataSave {
    /**
     * 设置顺序
     * @return
     */
     Integer getSort();

    /**
     * 设置处方默认数据 基类空实现 具体赋值子类处理
     *
     * @param recipe 处方头对象
     */
     void setRecipe(Recipe recipe) ;
    /**
     * 设置处方默认数据 基类空实现 具体赋值子类处理
     *
     * @param recipe 处方扩展对象
     */
     void setRecipeExt(Recipe recipe, RecipeExtend extend) ;

}
