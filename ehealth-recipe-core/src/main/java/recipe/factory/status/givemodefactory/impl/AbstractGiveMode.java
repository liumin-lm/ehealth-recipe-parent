package recipe.factory.status.givemodefactory.impl;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.factory.status.givemodefactory.IGiveModeService;
import recipe.factory.status.orderstatusfactory.RecipeOrderStatusProxy;

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

    @Override
    public void updateStatusAfter(UpdateOrderStatusVO orderStatus) {
    }
}
