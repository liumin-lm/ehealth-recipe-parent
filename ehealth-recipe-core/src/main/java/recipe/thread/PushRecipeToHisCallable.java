package recipe.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.service.RecipeHisService;

import java.util.concurrent.Callable;

/**
 * created by shiyuping on 2019/7/22
 */
public class PushRecipeToHisCallable implements Callable<String> {
    private Logger logger = LoggerFactory.getLogger(PushRecipeToHisCallable.class);

    private Integer recipeId;
    public PushRecipeToHisCallable(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Override
    public String call() throws Exception {
        logger.info("recipeSendHis thread start. recipeId={}",recipeId);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        //HIS消息发送
        hisService.recipeSendHis(recipeId, null);
        return null;
    }
}
