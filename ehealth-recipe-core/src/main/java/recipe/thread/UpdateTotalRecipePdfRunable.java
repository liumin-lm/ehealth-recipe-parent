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
import java.math.BigDecimal;

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
    private BigDecimal recipeFee;

    public UpdateTotalRecipePdfRunable(Integer recipeId, BigDecimal recipeFee) {
        this.recipeId = recipeId;
        this.recipeFee = recipeFee;
    }

    @Override
    public void run() {
        logger.info("UpdateTotalRecipePdfRunable start. recipeId={},actualPrice={}", recipeId, recipeFee);
        if (null == recipeFee) {
            return;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        //更新pdf
        if (null == recipe) {
            logger.warn("UpdateTotalRecipePdfRunable recipe is null  recipeId={}", recipeId);
            return;
        }

        try {
            String newPfd = null;
            String key = null;
            if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                newPfd = CreateRecipePdfUtil.generateTotalRecipePdf(recipe.getChemistSignFile(), String.valueOf(recipeFee), recipe.getRecipeType());
                key = "ChemistSignFile";
            } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                newPfd = CreateRecipePdfUtil.generateTotalRecipePdf(recipe.getChemistSignFile(), String.valueOf(recipeFee), recipe.getRecipeType());
                key = "SignFile";
            } else {
                logger.warn("UpdateTotalRecipePdfRunable file is null  recipeId={}", recipeId);
            }
            logger.info("UpdateTotalRecipePdfRunable file newPfd ={},key ={}", newPfd, key);
            if (StringUtils.isNotEmpty(newPfd) && StringUtils.isNotEmpty(key)) {
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of(key, newPfd));
            }
        } catch (DocumentException | IOException e) {
            logger.error("UpdateTotalRecipePdfRunable error recipeId={},e=", recipeId, e);
        }
    }
}
