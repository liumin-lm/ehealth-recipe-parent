package recipe.manager;

import com.ngari.recipe.entity.RecipeTherapy;
import org.springframework.stereotype.Service;
import recipe.dao.RecipeTherapyDAO;

import javax.annotation.Resource;

/**
 * 诊疗处方
 *
 * @author fuzi
 */
@Service
public class RecipeTherapyManager extends BaseManager {

    @Resource
    private RecipeTherapyDAO recipeTherapyDAO;

    public RecipeTherapy saveRecipeTherapy(RecipeTherapy recipeTherapy) {
        return recipeTherapy;
    }

    public RecipeTherapy getRecipeTherapyById(Integer id){
        return recipeTherapyDAO.getById(id);
    }
}
