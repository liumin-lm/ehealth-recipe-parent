package recipe.business;

import com.google.common.collect.ImmutableMap;
import com.ngari.recipe.entity.Recipe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.core.api.ICaBusinessService;
import recipe.dao.RecipeDAO;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.status.SignEnum;
import recipe.manager.StateManager;
import recipe.service.RecipeLogService;

/**
 * ca核心逻辑处理类
 *
 * @author liumin
 * @date 2022\3\15 0016 17:30
 */
@Service
public class CaBusinessService extends BaseService implements ICaBusinessService {
    @Autowired
    private StateManager stateManager;
    @Autowired
    private RecipeDAO recipeDAO;

    @Override
    public void signRecipeCAInterruptForStandard(Integer recipeId) {
        signRecipeCAInterrupt(recipeId, RecipeStatusEnum.RECIPE_STATUS_SIGN_ERROR_CODE_DOC, SignEnum.SIGN_STATE_AUDIT);
    }

    /**
     * @param recipeId
     * @param status
     * @param sign
     */
    @LogRecord
    public void signRecipeCAInterrupt(Integer recipeId, RecipeStatusEnum status, SignEnum sign) {
        //首先判断处方的装填是不是可以设置成需要重新中断的
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            logger.error("当前处方{}不存在", recipeId);
            return;
        }
        if (status.getType().equals(recipe.getStatus())) {
            return;
        }
        //将处方设置成医生签名失败
        stateManager.updateStatus(recipeId, status, sign);
        stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_SUBMIT, RecipeStateEnum.NONE);
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), status.getType(), sign.getName() + "设医生签名！");
    }


    @Override
    public void checkRecipeCAInterruptForStandard(Integer recipeId) {
        //首先判断处方的装填是不是可以设置成需要重新中断的
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

