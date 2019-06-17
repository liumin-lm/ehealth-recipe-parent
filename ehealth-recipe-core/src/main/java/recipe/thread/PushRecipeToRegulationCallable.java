package recipe.thread;

import com.google.common.collect.ImmutableMap;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.common.CommonConstant;
import recipe.common.response.CommonResponse;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.service.RecipeLogService;

import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * created by shiyuping on 2019/6/13
 */
public class PushRecipeToRegulationCallable implements Callable<String> {

    private Logger logger = LoggerFactory.getLogger(PushRecipeToRegulationCallable.class);

    private Integer recipeId;

    public PushRecipeToRegulationCallable(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Override
    public String call() throws Exception {
        if (recipeId==null){
            return null;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
        CommonResponse response = null;
        try {
            if (RecipeStatusConstant.CHECK_PASS_YS == recipe.getStatus()){
                response = service.uploadRecipeAuditIndicators(Arrays.asList(recipe));
                if (CommonConstant.SUCCESS.equals(response.getCode())){
                    response = service.uploadRecipeCirculationIndicators(Arrays.asList(recipe));
                } else{
                    logger.warn("uploadRecipeAuditIndicators rpc execute error. recipe={}", JSONUtils.toString(recipe));
                }
            }else {
                response = service.uploadRecipeIndicators(Arrays.asList(recipe));
            }
        } catch (Exception e) {
            logger.warn("uploadRecipeIndicators exception recipe={}", JSONUtils.toString(recipe), e);
        }
        logger.info("uploadRecipeIndicators res={}");
        if (CommonConstant.SUCCESS.equals(response.getCode())) {
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
        return null;
    }
}
