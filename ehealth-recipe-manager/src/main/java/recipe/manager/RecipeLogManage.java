package recipe.manager;

import org.springframework.stereotype.Service;

/**
 * 日志保存
 */
@Service
public class RecipeLogManage extends BaseManager{

    /**
     * 日志保存
     * @param recipeId  处方ID
     * @param beforeStatus 之前状态
     * @param afterStatus  之后状态
     * @param memo 备注
     */
    @Override
    public void saveRecipeLog(Integer recipeId, Integer beforeStatus, Integer afterStatus, String memo) {
        super.saveRecipeLog(recipeId, beforeStatus, afterStatus, memo);
    }

}
