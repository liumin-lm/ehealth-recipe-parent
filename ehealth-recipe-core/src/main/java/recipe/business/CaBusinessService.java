package recipe.business;

import com.google.common.collect.ImmutableMap;
import com.ngari.recipe.entity.Recipe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.ICaBusinessService;
import recipe.dao.RecipeDAO;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.service.RecipeLogService;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * ca核心逻辑处理类
 *
 * @author liumin
 * @date 2022\3\15 0016 17:30
 */
@Service
@Slf4j
public class CaBusinessService extends BaseService implements ICaBusinessService {

    @Autowired
    private RecipeDAO recipeDAO;

    @Override
    public void signRecipeCAInterruptForStandard(Integer recipeId) {
        //首先判断处方的装填是不是可以设置成需要重新中断的
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            logger.error("当前处方{}不存在", recipeId);
            return;
        }
        //将处方设置成医生签名失败
        Integer beforeStatus = recipe.getStatus();
        if (!RecipeStatusEnum.RECIPE_STATUS_UNSIGNED.getType().equals(recipe.getStatus())) {
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("status", RecipeStatusEnum.RECIPE_STATUS_SIGN_ERROR_CODE_DOC.getType()));
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), beforeStatus, RecipeStatusEnum.RECIPE_STATUS_SIGN_ERROR_CODE_DOC.getType(), "签名失败，设医生未签名！");
        }
    }

    @Override
    public void checkRecipeCAInterruptForStandard(Integer recipeId) {
        //首先判断处方的装填是不是可以设置成需要重新中断的
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            logger.error("当前处方{}不存在", recipeId);
            return;
        }
        //将处方设置成药师签名失败
        Integer beforeStatus = recipe.getStatus();
        if (!RecipeStatusEnum.RECIPE_STATUS_SIGN_NO_CODE_PHA.getType().equals(recipe.getStatus())) {
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("status", RecipeStatusEnum.RECIPE_STATUS_SIGN_ERROR_CODE_PHA.getType()));
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), beforeStatus, RecipeStatusEnum.RECIPE_STATUS_SIGN_ERROR_CODE_PHA.getType(), "签名失败，设置药师未签名！");
        }
    }

}

