package recipe.thread;

import recipe.service.RecipeHisService;
import recipe.util.ApplicationUtils;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * 更新从HIS获取到的处方状态
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/15.
 */
public class UpdateRecipeStatusFromHisCallable implements Callable<String> {

    private List<String> recipeCodes;

    private Integer organId;

    public UpdateRecipeStatusFromHisCallable(List<String> recipeCodes, Integer organId) {
        this.recipeCodes = recipeCodes;
        this.organId = organId;
    }

    @Override
    public String call() throws Exception {
        if (null == recipeCodes || recipeCodes.isEmpty() || null == organId) {
            return null;
        }

        //HIS消息发送
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        hisService.recipeListQuery(recipeCodes, organId);

        return null;
    }
}
