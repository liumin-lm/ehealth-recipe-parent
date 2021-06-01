package recipe.presettle.settle;

import com.ngari.his.recipe.mode.PayNotifyReqTO;
import com.ngari.his.recipe.mode.PayNotifyResTO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;

/**
 * created by shiyuping on 2020/12/7
 */
public interface IRecipeSettleService {
    /**
     * 处方结算
     * @param req
     * @return
     */
    PayNotifyResTO recipeSettle(PayNotifyReqTO req) throws Exception;

    /**
     * 结算返回值处理
     * @param res
     * @param recipe
     * @param result
     */
    void doRecipeSettleResponse(PayNotifyResTO res, Recipe recipe, RecipeResultBean result);
}
