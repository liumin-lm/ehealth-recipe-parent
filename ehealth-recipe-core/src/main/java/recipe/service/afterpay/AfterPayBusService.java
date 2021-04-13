package recipe.service.afterpay;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.util.AppContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author yinsheng
 * @date 2021\4\12 0012 17:26
 */
@Service
public class AfterPayBusService{

    public void handle(RecipeResultBean result, RecipeOrder recipeOrder, List<Recipe> recipes, Integer payFlag) {
        AfterPayBussType[] values = AfterPayBussType.values();
        for (AfterPayBussType bussType : values) {
            String serviceName = bussType.getName() + "Service";
            IAfterPayBussService afterPayBussService = AppContextHolder.getBean(serviceName, IAfterPayBussService.class);
            afterPayBussService.handle(result, recipeOrder, recipes, payFlag);
        }
    }
}
