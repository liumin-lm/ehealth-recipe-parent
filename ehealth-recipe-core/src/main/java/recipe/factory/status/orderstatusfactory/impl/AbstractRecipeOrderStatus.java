package recipe.factory.status.orderstatusfactory.impl;

import com.ngari.recipe.entity.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.factory.status.orderstatusfactory.IRecipeOrderStatusService;
import recipe.service.manager.GroupRecipeManager;

/**
 * 状态流转基类
 *
 * @author fuzi
 */
public abstract class AbstractRecipeOrderStatus implements IRecipeOrderStatusService {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    protected RecipeOrderDAO recipeOrderDAO;
    @Autowired
    protected RecipeDAO recipeDAO;
    @Autowired
    private GroupRecipeManager groupRecipeManager;

    @Override
    public void upRecipeThreadPool(Recipe recipe) {
    }

    @Override
    public void updateGroupRecipe(Recipe recipe, Integer orderId) {
        groupRecipeManager.updateGroupRecipe(recipe, orderId);
    }
}
