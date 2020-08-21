package recipe.thread;

import com.ngari.recipe.recipe.model.HisSendResTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.hisservice.RecipeToHisCallbackService;
import recipe.service.RecipeHisService;

import java.util.concurrent.Callable;

/**
 * @description ${description}
 * @author gmw
 * @date 2020/8/20
 * @param
 * @return
 */

public class RecipeSendSuccessRunnable implements Runnable {
//    private Logger logger = LoggerFactory.getLogger(RecipeSendSuccessRunnable.class);
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
