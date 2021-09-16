package recipe.thread;

import com.ngari.recipe.recipe.model.HisSendResTO;
import recipe.ApplicationUtils;
import recipe.hisservice.RecipeToHisCallbackService;

/**
 * @description ${description}
 * @author gmw
 * @date 2020/8/20
 * @param
 * @return
 */

public class RecipeSendSuccessRunnable implements Runnable {
    private HisSendResTO response;
    public RecipeSendSuccessRunnable(HisSendResTO response) {
        this.response = response;
    }
    @Override
    public void run()
    {
        RecipeToHisCallbackService service = ApplicationUtils.getRecipeService(RecipeToHisCallbackService.class);
        service.sendSuccess(response);
    }
}
