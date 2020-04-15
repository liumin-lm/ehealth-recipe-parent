package recipe.service;

import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.RecipeExtendDAO;

import java.util.Map;

/**
 * created by shiyuping on 2020/4/14
 */
@RpcBean
public class RecipeExtendService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeExtendService.class);

    @RpcService
    public Boolean updateRecipeExInfoByRecipeId(int recipeId,Map<String, ?> changeAttr){
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        LOGGER.info("updateRecipeExInfoByRecipeId recipeId={},changeAttr={}",recipeId, JSONUtils.toString(changeAttr));
        return recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeId,changeAttr);
    }
}
