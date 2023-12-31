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

public class RecipeSendFailRunnable implements Runnable {
    private HisSendResTO response;
    public RecipeSendFailRunnable(HisSendResTO response) {
        this.response = response;
    }
    @Override
    public void run() {
        RecipeToHisCallbackService service = ApplicationUtils.getRecipeService(RecipeToHisCallbackService.class);
        service.sendFail(response);
    }
}
