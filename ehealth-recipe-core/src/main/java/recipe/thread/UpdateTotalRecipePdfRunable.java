package recipe.thread;

import com.google.common.collect.ImmutableMap;
import com.itextpdf.text.DocumentException;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.dao.RecipeDAO;

import java.io.IOException;

/**
 * 修改pdf 添加费用
 *
 * @author fuzi
 */
public class UpdateTotalRecipePdfRunable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(UpdateTotalRecipePdfRunable.class);

    private Integer recipeId;
    /**
     * 实际支付费用
     */
    private Double actualPrice;

    public UpdateTotalRecipePdfRunable(Integer recipeId, Double actualPrice) {
        this.recipeId = recipeId;
        this.actualPrice = actualPrice;
    }

    @Override
    public void run() {
        logger.info("UpdateTotalRecipePdfRunable start. recipeId={},actualPrice={}", recipeId, actualPrice);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        //更新pdf
        if (null == recipe) {
            return;
        }

        try {
            if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                String newPfd = CreateRecipePdfUtil.generateTotalRecipePdf(recipe.getChemistSignFile(), actualPrice.toString(), recipe.getRecipeType());
                if (StringUtils.isNotEmpty(newPfd)) {
                    recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("ChemistSignFile", newPfd));
                }
            } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                String newPfd = CreateRecipePdfUtil.generateTotalRecipePdf(recipe.getChemistSignFile(), actualPrice.toString(), recipe.getRecipeType());
                if (StringUtils.isNotEmpty(newPfd)) {
                    recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("SignFile", newPfd));
                }
            }
        } catch (DocumentException e) {
            logger.error("UpdateTotalRecipePdfRunable error recipeId={},e=", recipeId, e);
        } catch (IOException e) {
            logger.error("UpdateTotalRecipePdfRunable error recipeId={},e=", recipeId, e);
        }
    }
}
