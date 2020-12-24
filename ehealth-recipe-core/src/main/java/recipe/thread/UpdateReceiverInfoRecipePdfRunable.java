package recipe.thread;

import com.google.common.collect.ImmutableMap;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.bussutil.openapi.util.JSONUtils;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.service.manager.RecipeLabelManager;

/**
 * 支付成功后修改pdf 添加收货人信息
 *
 * @author liumin
 */
public class UpdateReceiverInfoRecipePdfRunable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(UpdateReceiverInfoRecipePdfRunable.class);

    private final RecipeLabelManager recipeLabelManager;

    private final Integer recipeId;

    public UpdateReceiverInfoRecipePdfRunable(Integer recipeId, RecipeLabelManager recipeLabelManager) {
        this.recipeId = recipeId;
        this.recipeLabelManager = recipeLabelManager;
    }

    @Override
    public void run() {
        logger.info("UpdateReceiverInfoRecipePdfRunable start. recipeId={}", recipeId);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        //更新pdf
        if (null == recipe) {
            logger.warn("UpdateReceiverInfoRecipePdfRunable recipe is null  recipeId={}", recipeId);
            return;
        }
        try {
            String newPfd = null;
            String key = null;
            RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeOrder order = orderDAO.getRelationOrderByRecipeId(recipeId);
            CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
            logger.info("UpdateReceiverInfoRecipePdfRunable recipeid:{},order:{}", recipeId, JSONUtils.toString(order));
            //存在收货人信息
            if(order!=null&&(StringUtils.isNotEmpty(order.getReceiver()) || StringUtils.isNotEmpty(order.getRecMobile()) || StringUtils.isNotEmpty(commonRemoteService.getCompleteAddress(order)))){
                logger.info("UpdateReceiverInfoRecipePdfRunable recipeid:{} 添加收货人信息", recipeId);
                int height = recipeLabelManager.getPdfReceiverHeight(recipe.getClinicOrgan());
                if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                    newPfd = CreateRecipePdfUtil.generateReceiverInfoRecipePdf(recipe.getChemistSignFile(), order.getReceiver(), order.getRecMobile(), commonRemoteService.getCompleteAddress(order), height);
                    key = "ChemistSignFile";
                } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                    newPfd = CreateRecipePdfUtil.generateReceiverInfoRecipePdf(recipe.getSignFile(), order.getReceiver(), order.getRecMobile(), commonRemoteService.getCompleteAddress(order), height);
                    key = "SignFile";
                } else {
                    logger.warn("UpdateReceiverInfoRecipePdfRunable file is null  recipeId={}", recipeId);
                }
                logger.info("UpdateReceiverInfoRecipePdfRunable file recipeid:{},newPfd ={},key ={}",recipeId, newPfd, key);
                if (StringUtils.isNotEmpty(newPfd) && StringUtils.isNotEmpty(key)) {
                    recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of(key, newPfd));
                }
            }
        } catch (Exception e) {
            logger.error("UpdateReceiverInfoRecipePdfRunable error recipeId={},e=", recipeId, e);
        }
    }


}
