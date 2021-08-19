package recipe.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.*;

/**
 * his调用基类
 *
 * @author fuzi
 */
public class BaseManager {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    protected RecipeDAO recipeDAO;
    @Autowired
    protected RecipeExtendDAO recipeExtendDAO;
    @Autowired
    protected RecipeDetailDAO recipeDetailDAO;
    @Autowired
    protected RecipeOrderDAO recipeOrderDAO;
    @Autowired
    protected OrganDrugListDAO organDrugListDAO;
}
