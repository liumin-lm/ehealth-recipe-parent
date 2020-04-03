package recipe.service.recipecancel;

import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.service.RecipeServiceSub;

import java.util.Map;

/**
 * created by shiyuping on 2020/4/3
 * 处方撤销服务类
 */
@RpcBean("recipeCancelService")
public class RecipeCancelService {

    /**
     * 处方撤销处方new----------(供医生端使用)
     *
     * @param recipeId 处方Id
     * @param message 处方撤销原因
     * @return Map<String ,   Object>
     */
    @RpcService
    public Map<String, Object> cancelRecipe(Integer recipeId, String message) {
        return RecipeServiceSub.cancelRecipeImpl(recipeId, 0, "", message);
    }
}
