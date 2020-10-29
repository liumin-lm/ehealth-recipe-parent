package recipe.factory.status.orderstatusfactory.impl;

import com.ngari.recipe.entity.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.factory.status.orderstatusfactory.IRecipeOrderStatusService;

/**
 * 状态流转基类
 *
 * @author fuzi
 */
public abstract class AbstractRecipeOrderStatus implements IRecipeOrderStatusService {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * 订单是否有效 1有效，0表示该订单已取消或者无效临时订单
     */
    protected final static int EFFECTIVE = 0;

    @Autowired
    protected RecipeOrderDAO recipeOrderDAO;
    @Autowired
    protected RecipeDAO recipeDAO;

    protected Recipe getRecipe(Integer recipeId) {
        return recipeDAO.getByRecipeId(recipeId);
    }

    @Override
    public void upRecipeThreadPool(Recipe recipe) {
    }
}
