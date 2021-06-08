package recipe.thread;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.bussutil.openapi.request.province.SignImgNode;
import recipe.dao.RecipeDAO;

/**
 * 为处方pdf盖章
 *
 * @author fuzi
 */
public class GenerateSignetRecipePdfRunable implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(GenerateSignetRecipePdfRunable.class);

    /**
     * 处方id
     */
    private Integer recipeId;
    /**
     * 机构id
     */
    private Integer organId;

    public GenerateSignetRecipePdfRunable(Integer recipeId, Integer organId) {
        this.recipeId = recipeId;
        this.organId = organId;
    }

    @Override
    public void run() {
        logger.info("GenerateSignetRecipePdfRunable start recipeId={}, organId={}", recipeId, organId);
        //获取配置--机构印章
        IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        Object organSealId = configurationCenterUtilsService.getConfiguration(organId, "organSeal");
        if (null == organSealId || StringUtils.isEmpty(organSealId.toString())) {
            logger.info("GenerateSignetRecipePdfRunable organSeal is null");
            return;
        }
        //获取 处方数据
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        if (null == recipe || StringUtils.isEmpty(recipe.getChemistSignFile())) {
            logger.info("GenerateSignetRecipePdfRunable recipe is null");
            return;
        }

        try {
            //更新pdf
            SignImgNode signImgNode = new SignImgNode(recipe.getRecipeId().toString(), recipe.getRecipeId().toString()
                    , organSealId.toString(), recipe.getChemistSignFile(), 90F, 90F, 160f, 490f);
            String newPfd = CreateRecipePdfUtil.generateSignImgNode(signImgNode);
            if (StringUtils.isNotEmpty(newPfd)) {
                Recipe recipeUpdate = new Recipe();
                recipeUpdate.setRecipeId(recipeId);
                recipeUpdate.setChemistSignFile(newPfd);
                recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
            }
            logger.info("GenerateSignetRecipePdfRunable end newPfd={}, organSealId={}", newPfd, organSealId);
        } catch (Exception e) {
            logger.error("GenerateSignetRecipePdfRunable error recipeId={}, e={}", recipeId, e);
        }
    }
}
