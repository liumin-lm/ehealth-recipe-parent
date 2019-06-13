package recipe.hisservice.syncdata;

import com.google.common.collect.ImmutableMap;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.common.CommonConstant;
import recipe.common.response.CommonResponse;
import recipe.dao.RecipeDAO;
import recipe.service.RecipeLogService;

import java.util.Arrays;

/**
 * @author： 0184/yu_yun
 * @date： 2019/2/18
 * @description： 与监管平台数据同步执行服务
 * @version： 1.0
 */
@RpcBean("syncExecutorService")
public class SyncExecutorService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncExecutorService.class);

    /**
     * 上传批量处方，暂时用不到
     */
    @RpcService
    public void uploadRecipeIndicators() {
        
    }

    /**
     * 上传单个处方
     *
     * @param recipe
     */
    public void uploadRecipeIndicators(Recipe recipe) {
        CommonSyncSupervisionService service = ApplicationUtils.getRecipeService(CommonSyncSupervisionService.class);
        CommonSyncSupervisionForIHosService iHosService =
                ApplicationUtils.getRecipeService(CommonSyncSupervisionForIHosService.class);
        CommonResponse response = null;
        try {
            //RPC调用上传
            response = iHosService.uploadRecipeIndicators(Arrays.asList(recipe));
            if (CommonConstant.SUCCESS.equals(response.getCode())){
                //上传openApi的
                response = service.uploadRecipeIndicators(Arrays.asList(recipe));
            } else{
                LOGGER.warn("uploadRecipeIndicators rpc execute error. recipe={}", JSONUtils.toString(recipe));
            }
        } catch (Exception e) {
            LOGGER.warn("uploadRecipeIndicators exception recipe={}", JSONUtils.toString(recipe), e);
        }
        if (CommonConstant.SUCCESS.equals(response.getCode())) {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            //更新字段
            recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("syncFlag", 1));
            //记录日志
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(),
                    recipe.getStatus(), "监管平台上传成功");
        }else{
            //记录日志
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(),
                    recipe.getStatus(), "监管平台上传失败,"+response.getMsg());
        }
        
    }

    /**
     * 上传核销信息
     * @param recipeId
     */
    public void uploadRecipeVerificationIndicators(int recipeId){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        if(null == recipe){
            LOGGER.warn("uploadRecipeVerificationIndicators recipe is null. recipeId={}", recipeId);
            return;
        }

        CommonSyncSupervisionService service = ApplicationUtils.getRecipeService(CommonSyncSupervisionService.class);
        CommonSyncSupervisionForIHosService iHosService =
                ApplicationUtils.getRecipeService(CommonSyncSupervisionForIHosService.class);
        CommonResponse response = null;
        try {
            //RPC调用上传
            response = iHosService.uploadRecipeVerificationIndicators(Arrays.asList(recipe));
            if (CommonConstant.SUCCESS.equals(response.getCode())){
                //上传openApi的
                response = service.uploadRecipeVerificationIndicators(Arrays.asList(recipe));
            } else{
                LOGGER.warn("uploadRecipeVerificationIndicators rpc execute error. recipe={}", JSONUtils.toString(recipe));
            }
        } catch (Exception e) {
            LOGGER.warn("uploadRecipeVerificationIndicators exception recipe={}", JSONUtils.toString(recipe), e);
        }
        if (CommonConstant.SUCCESS.equals(response.getCode())) {
            //记录日志
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(),
                    recipe.getStatus(), "监管平台上传核销信息成功");
        }else{
            //记录日志
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(),
                    recipe.getStatus(), "监管平台上传核销信息失败,"+response.getMsg());
        }
    }

}
