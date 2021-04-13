package recipe.service.afterpay;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import org.springframework.stereotype.Component;
import recipe.thread.CardDataUploadRunable;
import recipe.thread.RecipeBusiThreadPool;

import java.util.List;

/**
 * 健康卡上传业务
 * @author yinsheng
 * @date 2021\4\13 0013 09:28
 */
@Component("healthCardService")
public class HealthCardService implements IAfterPayBussService{

    @Override
    public void handle(RecipeResultBean result, RecipeOrder recipeOrder, List<Recipe> recipes, Integer payFlag) {
        //健康卡数据上传
        RecipeBusiThreadPool.execute(new CardDataUploadRunable(recipes.get(0).getClinicOrgan(), recipes.get(0).getMpiid(), "030102"));
    }
}
