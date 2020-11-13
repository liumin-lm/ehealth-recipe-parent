package recipe.thread;

import recipe.ApplicationUtils;
import recipe.service.RecipeHisService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 更新从HIS获取到的处方状态
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2016/6/15.
 */
public class UpdateRecipeStatusFromHisCallable implements Callable<String> {

    /*private List<String> recipeCodes;

    private Integer organId;*/

    private Map<Integer, List<String>> map;

    public UpdateRecipeStatusFromHisCallable(Map<Integer, List<String>> map) {
        /*this.recipeCodes = recipeCodes;
        this.organId = organId;*/
        this.map = map;
    }

    @Override
    public String call() throws Exception {
        for (Integer organId : map.keySet()) {
            if (null == map.get(organId) || map.get(organId).isEmpty() || null == organId) {
                continue;
            }
            //HIS消息发送
            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
            hisService.recipeListQuery(map.get(organId), organId);
        }
        return "";
    }
}
