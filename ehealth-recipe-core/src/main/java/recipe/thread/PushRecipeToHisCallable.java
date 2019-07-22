package recipe.thread;

import recipe.ApplicationUtils;
import recipe.service.RecipeHisService;

import java.util.concurrent.Callable;

/**
 * created by shiyuping on 2019/7/22
 */
public class PushRecipeToHisCallable implements Callable<String> {

    private Integer recipeId;
    public PushRecipeToHisCallable(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Override
    public String call() throws Exception {
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        //HIS消息发送
        hisService.recipeSendHis(recipeId, null);
        return null;
    }
}
