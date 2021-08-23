package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.*;

import java.util.List;

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


    /**
     * 获取处方相关信息
     *
     * @param recipeId 处方id
     * @return
     */
    protected RecipeDTO getRecipeDTO(Integer recipeId) {
        logger.info("BaseManager getRecipeDTO recipeId:{}", recipeId);
        RecipeDTO recipeDTO = new RecipeDTO();
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        recipeDTO.setRecipe(recipe);
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        recipeDTO.setRecipeDetails(recipeDetails);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        recipeDTO.setRecipeExtend(recipeExtend);
        logger.info("BaseManager getRecipeDTO recipeDTO:{}", JSON.toJSONString(recipeDTO));
        return recipeDTO;
    }
}
