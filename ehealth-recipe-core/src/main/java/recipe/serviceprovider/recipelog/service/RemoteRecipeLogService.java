package recipe.serviceprovider.recipelog.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.RecipeLog;
import com.ngari.recipe.recipelog.model.RecipeLogBean;
import com.ngari.recipe.recipelog.service.IRecipeLogService;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.ApplicationUtils;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeLogDAO;
import recipe.service.RecipeLogService;
import recipe.serviceprovider.BaseService;

import java.util.List;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/4.
 */
@RpcBean("remoteRecipeLogService")
public class RemoteRecipeLogService extends BaseService<RecipeLogBean> implements IRecipeLogService {

    @RpcService
    @Override
    public void saveRecipeLogEx(RecipeLogBean recipeLogBean) {
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

    @Override
    @RpcService
    public List<RecipeLogBean> findByRecipeId(Integer recipeId){
        RecipeLogService service = ApplicationUtils.getRecipeService(RecipeLogService.class);
        return service.findByRecipeId(recipeId);
    }

    @Override
    public List<RecipeLogBean> findByRecipeIdAndAfterStatus(Integer recipeId, Integer afterStatus) {
        RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);
        List<RecipeLog> recipeLogs = recipeLogDAO.findByRecipeIdAndAfterStatusDesc(recipeId, afterStatus);
        return ObjectCopyUtils.convert(recipeLogs,RecipeLogBean.class);
    }
}
