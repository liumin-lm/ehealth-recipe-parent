package recipe.thread;

import com.aliyun.openservices.shade.com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.client.IConfigurationClient;
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
    private static final Logger logger = LoggerFactory.getLogger(RecipeSendSuccessRunnable.class);
    private List<Recipedetail> recipeDetails;
    private Recipe recipe;

    public RecipeSendSuccessRunnable(List<Recipedetail> recipeDetails, Recipe recipe) {
        this.recipeDetails = recipeDetails;
        this.recipe = recipe;
    }

    @Override
    public void run() {
        logger.info("RecipeSendSuccessRunnable run recipeDetails={}", JSON.toJSONString(recipeDetails));
        /**更新药品最新的价格等*/
        IConfigurationClient configurationClient = AppContextHolder.getBean("IConfigurationClient", IConfigurationClient.class);
        boolean recipeSendUpdatePrice = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "recipeSendUpdatePrice", false);
        if (recipeSendUpdatePrice) {
            OrganDrugListService organDrugListService = ApplicationUtils.getRecipeService(OrganDrugListService.class);
            recipeDetails.forEach(a -> organDrugListService.saveOrganDrug(recipe.getClinicOrgan(), a));
        }
        //将药品信息加入电子病历中
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        EmrRecipeManager emrRecipeManager = AppContextHolder.getBean("emrRecipeManager", EmrRecipeManager.class);
        emrRecipeManager.upDocIndex(recipeExtend.getRecipeId(), recipeExtend.getDocIndexId());
    }
}
