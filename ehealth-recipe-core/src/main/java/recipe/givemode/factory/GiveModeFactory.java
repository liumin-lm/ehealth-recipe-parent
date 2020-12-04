package recipe.givemode.factory;

import ctd.util.AppContextHolder;
import recipe.constant.RecipeBussConstant;

/**
 * @author yinsheng
 * @date 2020\12\3 0003 19:49
 */
public class GiveModeFactory {

    public static IGiveModeBase getGiveModeBaseByRecipeMode(String recipeMode){
        IGiveModeBase giveModeBase;

        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipeMode)) {
            giveModeBase = AppContextHolder.getBean("ngariHealthGiveModeService", NgariHealthGiveModeService.class);
        } else if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)) {
            giveModeBase = AppContextHolder.getBean("zjsGiveModeService", ZjsGiveModeService.class);
        } else {
            //默认走平台的
            giveModeBase = AppContextHolder.getBean("ngariHealthGiveModeService", NgariHealthGiveModeService.class);
        }
        return giveModeBase;
    }
}
