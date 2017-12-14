package recipe.service;

import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.hibernate.mapping.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.RecipeDAO;
import recipe.drugsenterprise.ThirdEnterpriseCallService;
import recipe.util.ApplicationUtils;
import recipe.util.DateConversion;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 电子处方定时任务服务
 */
@RpcBean("recipeTimedTaskService")
public class RecipeTimedTaskService {

    private static final Logger logger = LoggerFactory.getLogger(RecipeTimedTaskService.class);

    /**
     * 定时任务 钥匙圈处方 配送中状态 持续一周后系统自动完成该笔业务
     */
    @RpcService
    public void autoFinishRecipeTask() {
        String endDt =
                DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(RecipeSystemConstant.ONE_WEEK_AGO),
                        DateConversion.DEFAULT_DATE_TIME);
        String startDt =
                DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(
                        RecipeSystemConstant.ONE_MONTH_AGO), DateConversion.DEFAULT_DATE_TIME);

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Recipe> recipes = recipeDAO.findNotConfirmReceiptList(startDt, endDt);

        ThirdEnterpriseCallService service = ApplicationUtils.getRecipeService(
                ThirdEnterpriseCallService.class, "takeDrugService");

        if (null != recipes && recipes.size() > 0) {
            for (Recipe recipe : recipes) {
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("recipeId", recipe.getRecipeId());
                paramMap.put("sender", "systemTask");
                service.finishRecipe(paramMap);
            }
        }
        logger.info("autoFinishRecipeTask size={}", recipes.size());
    }
}
