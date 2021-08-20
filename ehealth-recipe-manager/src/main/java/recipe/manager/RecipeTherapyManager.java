package recipe.manager;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeTherapy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.RecipeTherapyDAO;
import recipe.util.ValidateUtil;

/**
 * 诊疗处方
 *
 * @author fuzi
 */
@Service
public class RecipeTherapyManager extends BaseManager {
    @Autowired
    private RecipeTherapyDAO recipeTherapyDAO;

    public RecipeTherapy saveRecipeTherapy(RecipeTherapy recipeTherapy, Recipe recipe) {
        recipeTherapy.setDoctorId(recipe.getDoctor());
        recipeTherapy.setMpiId(recipe.getMpiid());
        recipeTherapy.setRecipeId(recipe.getRecipeId());
        if (ValidateUtil.integerIsEmpty(recipeTherapy.getId())) {
            recipeTherapy = recipeTherapyDAO.save(recipeTherapy);
        } else {
            recipeTherapy = recipeTherapyDAO.update(recipeTherapy);
        }
        return recipeTherapy;
    }

    public RecipeTherapy getRecipeTherapyById(Integer id) {
        return recipeTherapyDAO.getById(id);
    }

    public RecipeTherapy getRecipeTherapyByRecipeId(Integer recipeId) {
        return recipeTherapyDAO.getByRecipeId(recipeId);
    }

    public RecipeTherapy updateRecipeTherapy(RecipeTherapy recipeTherapy) {
        return recipeTherapyDAO.update(recipeTherapy);
    }
}
