package recipe.serviceprovider.recipelog.service;

import com.ngari.recipe.entity.RecipeLog;
import com.ngari.recipe.recipelog.model.RecipeLogBean;
import com.ngari.recipe.recipelog.service.IRecipeLogService;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.service.RecipeLogService;
import recipe.serviceprovider.BaseService;
import recipe.util.ApplicationUtils;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/4.
 */
@RpcBean("remoteRecipeLogService")
public class RemoteRecipeLogService extends BaseService<RecipeLogBean> implements IRecipeLogService {

    @RpcService
    @Override
    public void saveRecipeLog(RecipeLogBean recipeLogBean) {
        RecipeLog log = getBean(recipeLogBean, RecipeLog.class);
        RecipeLogService service = ApplicationUtils.getRecipeService(RecipeLogService.class);
        service.saveRecipeLog(log);
    }

    @RpcService
    @Override
    public void saveRecipeLog(int recipeId, int beforeStatus, int afterStatus, String memo) {
        RecipeLogService service = ApplicationUtils.getRecipeService(RecipeLogService.class);
        service.saveRecipeLog(recipeId, beforeStatus, afterStatus, memo);
    }
}
