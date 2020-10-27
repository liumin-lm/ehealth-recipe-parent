package recipe.status.factory.givemodefactory.impl;

import com.ngari.recipe.entity.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.status.factory.givemodefactory.IGiveModeService;
import recipe.status.factory.orderstatusfactory.RecipeOrderStatusProxy;

/**
 * 发药方式基类
 *
 * @author fuzi
 */
@Service
public abstract class AbstractGiveMode implements IGiveModeService {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    protected RecipeOrderDAO recipeOrderDAO;

    @Autowired
    protected RecipeDAO recipeDAO;
    @Autowired
    protected RecipeOrderStatusProxy recipeOrderStatusProxy;

    protected Recipe getRecipe(Integer recipeId) {
        return recipeDAO.getByRecipeId(recipeId);
    }
}
