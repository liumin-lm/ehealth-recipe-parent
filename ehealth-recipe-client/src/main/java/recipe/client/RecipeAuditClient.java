package recipe.client;

import ctd.util.JSONUtils;
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.api.IRecipeCheckService;
import eh.recipeaudit.model.RecipeCheckBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 审方相关服务
 *
 * @Author liumin
 * @Date 2021/7/22 下午2:26
 * @Description
 */
@Service
public class RecipeAuditClient extends BaseClient {

    @Autowired
    private IRecipeCheckService recipeCheckService;

    @Autowired
    private IRecipeAuditService recipeAuditService;


    /**
     * 通过处方号获取审方信息
     *
     * @param recipeId
     * @return
     */
    public RecipeCheckBean getByRecipeId(Integer recipeId) {
        logger.info("RecipeAuditClient getByRecipeId param recipeId:{}", recipeId);
        RecipeCheckBean recipeCheck = recipeCheckService.getByRecipeId(recipeId);
        logger.info("RecipeAuditClient getByRecipeId res recipeCheck:{} ", JSONUtils.toString(recipeCheck));
        return recipeCheck;
    }

    /**
     * 获取审核不通过详情
     *
     * @param recipeId
     * @return
     */
    public List<Map<String, Object>> getCheckNotPassDetail(Integer recipeId) {
        logger.info("RecipeAuditClient getCheckNotPassDetail param recipeId:{}", recipeId);
        List<Map<String, Object>> mapList = recipeAuditService.getCheckNotPassDetail(recipeId);
        logger.info("RecipeAuditClient getCheckNotPassDetail res mapList:{}", JSONUtils.toString(mapList));
        return mapList;
    }


}
