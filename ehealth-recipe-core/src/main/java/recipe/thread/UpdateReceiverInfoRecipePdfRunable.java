package recipe.thread;

import com.google.common.collect.ImmutableMap;
import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.bussutil.openapi.util.JSONUtils;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.service.manager.RecipeLabelManager;

/**
 * 支付成功后修改pdf 添加收货人信息/煎法
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
            RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeOrder order = orderDAO.getRelationOrderByRecipeId(recipeId);
            if (null == order) {
                logger.warn("UpdateReceiverInfoRecipePdfRunable order is null  recipeId={}", recipeId);
                return;
            }
            String newPfd = null;
            String key = null;
            //decoctionDeploy 煎法
            CoOrdinateVO decoction = validateDecoction(recipe);
            CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
            logger.info("UpdateReceiverInfoRecipePdfRunable recipeid:{},order:{}", recipeId, JSONUtils.toString(order));
            //存在收货人信息
            if (StringUtils.isNotEmpty(order.getReceiver()) || StringUtils.isNotEmpty(order.getRecMobile())) {
                CoOrdinateVO coOrdinateVO = recipeLabelManager.getPdfCoordsHeight(recipe.getRecipeId(), "receiverPlaceholder");
                if (null == coOrdinateVO) {
                    return;
                }
                if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                    newPfd = CreateRecipePdfUtil.generateReceiverInfoRecipePdf(recipe.getChemistSignFile(), order.getReceiver(), order.getRecMobile(), commonRemoteService.getCompleteAddress(order), coOrdinateVO.getY(), decoction);
                    key = "ChemistSignFile";
                } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                    newPfd = CreateRecipePdfUtil.generateReceiverInfoRecipePdf(recipe.getSignFile(), order.getReceiver(), order.getRecMobile(), commonRemoteService.getCompleteAddress(order), coOrdinateVO.getY(), decoction);
                    key = "SignFile";
                }
            } else if (null != decoction) {
                if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                    newPfd = CreateRecipePdfUtil.generateCoOrdinatePdf(recipe.getSignFile(), decoction);
                    key = "ChemistSignFile";
                } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                    newPfd = CreateRecipePdfUtil.generateCoOrdinatePdf(recipe.getSignFile(), decoction);
                    key = "SignFile";
                }
            }
            if (StringUtils.isNotEmpty(newPfd) && StringUtils.isNotEmpty(key)) {
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of(key, newPfd));
            }
            logger.info("UpdateReceiverInfoRecipePdfRunable file recipeid:{},newPfd ={},key ={}", recipeId, newPfd, key);
        } catch (Exception e) {
            logger.error("UpdateReceiverInfoRecipePdfRunable error recipeId={}", recipeId, e);
        }
    }

    /**
     * 校验煎法
     *
     * @param recipe
     * @return
     */
    private CoOrdinateVO validateDecoction(Recipe recipe) {
        IConfigurationCenterUtilsService iConfigService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        Object configOrderType = iConfigService.getConfiguration(recipe.getClinicOrgan(), "decoctionDeploy");
        if (null == configOrderType) {
            return null;
        }
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (null == recipeExtend || StringUtils.isEmpty(recipeExtend.getDecoctionText())) {
            return null;
        }
        //decoctionDeploy 煎法
        CoOrdinateVO coOrdinateVO = recipeLabelManager.getPdfCoordsHeight(recipe.getRecipeId(), "tcmDecoction");
        if (null == coOrdinateVO) {
            return null;
        }
        coOrdinateVO.setValue(recipeExtend.getDecoctionText());
        return coOrdinateVO;
    }

}
