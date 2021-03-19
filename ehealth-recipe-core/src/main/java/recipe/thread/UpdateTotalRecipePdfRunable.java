package recipe.thread;

import com.google.common.collect.ImmutableMap;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import eh.entity.base.Scratchable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.dao.RecipeDAO;
import recipe.service.manager.RecipeLabelManager;

import java.math.BigDecimal;
import java.util.List;

/**
 * 修改pdf 添加费用
 *
 * @author fuzi
 */
public class UpdateTotalRecipePdfRunable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(UpdateTotalRecipePdfRunable.class);
    private final RecipeLabelManager recipeLabelManager;

    private Integer recipeId;
    /**
     * 实际支付费用
     */
    private BigDecimal recipeFee;

    public UpdateTotalRecipePdfRunable(RecipeLabelManager recipeLabelManager, Integer recipeId, BigDecimal recipeFee) {
        this.recipeId = recipeId;
        this.recipeFee = recipeFee;
        this.recipeLabelManager = recipeLabelManager;
    }

    @Override
    public void run() {
        logger.info("UpdateTotalRecipePdfRunable start. recipeId={},recipeFee={}", recipeId, recipeFee);
        if (null == recipeFee) {
            logger.warn("UpdateTotalRecipePdfRunable recipeFee is null  recipeFee={}", recipeFee);
            return;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        //更新pdf
        if (null == recipe) {
            logger.warn("UpdateTotalRecipePdfRunable recipe is null  recipeId={}", recipeId);
            return;
        }
        List<Scratchable> scratchableList = recipeLabelManager.scratchableList(recipe.getClinicOrgan(), "moduleFour");
        if (CollectionUtils.isEmpty(scratchableList)) {
            return;
        }
        boolean actualPrice = scratchableList.stream().noneMatch(a -> "recipe.actualPrice".equals(a.getBoxLink()));
        if (actualPrice) {
            return;
        }

        try {
            String newPfd = null;
            String key = null;
            if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                newPfd = CreateRecipePdfUtil.generateTotalRecipePdf(recipe.getChemistSignFile(), String.valueOf(recipeFee));
                key = "ChemistSignFile";
            } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                newPfd = CreateRecipePdfUtil.generateTotalRecipePdf(recipe.getSignFile(), String.valueOf(recipeFee));
                key = "SignFile";
            } else {
                logger.warn("UpdateTotalRecipePdfRunable file is null  recipeId={}", recipeId);
            }
            logger.info("UpdateTotalRecipePdfRunable file newPfd ={},key ={}", newPfd, key);
            if (StringUtils.isNotEmpty(newPfd) && StringUtils.isNotEmpty(key)) {
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of(key, newPfd));
            }
        } catch (Exception e) {
            logger.error("UpdateTotalRecipePdfRunable error recipeId={},e=", recipeId, e);
        }
    }
}
