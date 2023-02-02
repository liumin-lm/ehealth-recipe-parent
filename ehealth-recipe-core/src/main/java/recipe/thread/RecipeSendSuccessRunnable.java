package recipe.thread;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import recipe.ApplicationUtils;
import recipe.dao.RecipeExtendDAO;
import recipe.manager.EmrRecipeManager;
import recipe.service.OrganDrugListService;

import java.util.List;

/**
 * @param
 * @author gmw
 * @description ${description}
 * @date 2020/8/20
 * @return
 */

public class RecipeSendSuccessRunnable implements Runnable {
    private List<Recipedetail> recipeDetails;
    private Recipe recipe;

    public RecipeSendSuccessRunnable(List<Recipedetail> recipeDetails, Recipe recipe) {
        this.recipeDetails = recipeDetails;
        this.recipe = recipe;
    }

    @Override
    public void run() {

        /**更新药品最新的价格等*/
        OrganDrugListService organDrugListService = ApplicationUtils.getRecipeService(OrganDrugListService.class);
        recipeDetails.forEach(a -> organDrugListService.saveOrganDrug(recipe.getClinicOrgan(), a));

        //将药品信息加入电子病历中
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        EmrRecipeManager emrRecipeManager = AppContextHolder.getBean("emrRecipeManager", EmrRecipeManager.class);
        emrRecipeManager.upDocIndex(recipeExtend.getRecipeId(), recipeExtend.getDocIndexId());


//        RecipeToHisCallbackService service = ApplicationUtils.getRecipeService(RecipeToHisCallbackService.class);
//        service.sendSuccess(response);
    }
}
