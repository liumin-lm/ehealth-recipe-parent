package recipe.givemode.bussess;

import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import org.apache.commons.lang3.StringUtils;
import recipe.constant.RecipeBussConstant;
import recipe.dao.HisRecipeDAO;

/**
 * @author yinsheng
 * @date 2020\12\3 0003 19:49
 */
public class GiveModeFactory {

    public static IGiveModeBase getGiveModeBaseByRecipeMode(Recipe recipe){
        IGiveModeBase giveModeBase;

        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
            if (new Integer(2).equals(recipe.getRecipeSource())) {
                //表示来源于线下转线上的处方单
                HisRecipeDAO hisRecipeDAO = DAOFactory.getDAO(HisRecipeDAO.class);
                HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
                //只有北京互联网医院DeliveryCode是不为空的
                if (hisRecipe != null && StringUtils.isNotEmpty(hisRecipe.getDeliveryCode())) {
                    return AppContextHolder.getBean("bjGiveModeService", BjGiveModeService.class);
                }
            }
            giveModeBase = AppContextHolder.getBean("ngariHealthGiveModeService", NgariHealthGiveModeService.class);
        } else if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
            giveModeBase = AppContextHolder.getBean("zjsGiveModeService", ZjsGiveModeService.class);
        } else {
            //默认走平台的
            giveModeBase = AppContextHolder.getBean("ngariHealthGiveModeService", NgariHealthGiveModeService.class);
        }
        return giveModeBase;
    }
}
