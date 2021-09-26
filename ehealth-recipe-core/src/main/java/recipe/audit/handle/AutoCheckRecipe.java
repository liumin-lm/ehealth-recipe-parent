package recipe.audit.handle;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditPreMode;
import recipe.audit.service.PrescriptionService;
import recipe.dao.RecipeDetailDAO;
import recipe.manager.RecipeManager;
import recipe.service.RecipeService;

import java.util.List;

/**
 * @desc 三方自动审核
 * @author maoze
 */
@Component
public class AutoCheckRecipe {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoCheckRecipe.class);

    private static RecipeManager recipeManager;

    @Autowired
    public AutoCheckRecipe(RecipeManager recipeManager) {
        AutoCheckRecipe.recipeManager = recipeManager;
    }

    /**
     * @desc 获取完整的处方和病历信息
     * @author maoze
     * @param recipeId 
     * @return
     */
    public static Recipe getByRecipeId(Integer recipeId){
       return recipeManager.getRecipeById(recipeId);
    }

    /**
     * @return
     */
    public static Boolean threeRecipeAutoCheck(Integer recipeId,Integer organId){
        LOGGER.info("threeRecipeAutoCheck recipe={}", recipeId);
        try {
            IConfigurationCenterUtilsService iConfigService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            Integer intellectJudicialFlag = (Integer) iConfigService.getConfiguration(organId, "intellectJudicialFlag");
            String autoRecipecheckLevel = (String) iConfigService.getConfiguration(organId, "autoRecipecheckLevel");
            String defaultRecipecheckDoctor = (String) iConfigService.getConfiguration(organId, "defaultRecipecheckDoctor");
            if (intellectJudicialFlag == 3
                    && StringUtils.isNotEmpty(defaultRecipecheckDoctor) && StringUtils.isNotEmpty(autoRecipecheckLevel)) {
                // 这个只是一个范围判断
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("threeRecipeAutoCheck error recipe={}", recipeId, e);
            return false;
        }
    }

    /**
     * @desc 执行具体的三方 只能是三方审核模式下
     * @param recipeId
     */
    public static void doAutoRecipe(Integer recipeId){
        LOGGER.info("doAutoRecipe:start:param={}",recipeId);
        PrescriptionService prescriptionService = ApplicationUtils.getRecipeService(PrescriptionService.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        RecipeBean recipeBean = recipeService.getByRecipeId(recipeId);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<RecipeDetailBean> list = ObjectCopyUtils.convert(recipeDetailDAO.findByRecipeId(recipeId),RecipeDetailBean.class);
        prescriptionService.analysis(recipeBean, list);
        LOGGER.info("doAutoRecipe:end");
    }

}
