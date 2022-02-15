package recipe.factory.status.orderstatusfactory.impl;

import com.ngari.recipe.entity.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeOrderBillDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.factory.status.orderstatusfactory.IRecipeOrderStatusService;
import recipe.manager.GroupRecipeManager;
import recipe.manager.StateManager;

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
    protected RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private GroupRecipeManager groupRecipeManager;
    @Autowired
    protected RecipeOrderBillDAO recipeOrderBillDAO;
    @Autowired
    protected StateManager stateManager;

    @Override
    public void upRecipeThreadPool(Recipe recipe) {
    }

    @Override
    public void updateGroupRecipe(Recipe recipe, Integer orderId) {
        groupRecipeManager.updateGroupRecipe(recipe, orderId);
    }
}
