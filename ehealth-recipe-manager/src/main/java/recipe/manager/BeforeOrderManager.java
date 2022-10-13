package recipe.manager;

import com.ngari.recipe.entity.RecipeBeforeOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.IConfigurationClient;
import recipe.common.CommonConstant;
import recipe.dao.RecipeBeforeOrderDAO;
import recipe.enumerate.status.YesOrNoEnum;

import java.util.Objects;

/**
 * @description：
 * @author： whf
 * @date： 2022-10-12 17:24
 */
@Service
public class BeforeOrderManager extends BaseManager {
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private RecipeBeforeOrderDAO recipeBeforeOrderDAO;

    /**
     * 上海六院写入his成功之后锁定购药方式
     * @param recipeId
     * @param pushType
     */
    public void updateRecipeHisStatus(Integer organId,Integer recipeId, Integer pushType) {
        if (!CommonConstant.RECIPE_PUSH_TYPE.equals(pushType)) {
            return;
        }
        Boolean isUpdateRecipeGiveMode = configurationClient.getValueBooleanCatch(organId, "isUpdateRecipeGiveMode", true);
        if(isUpdateRecipeGiveMode){
            return;
        }
        RecipeBeforeOrder recipeBeforeOrder = recipeBeforeOrderDAO.getRecipeBeforeOrderByRecipeId(recipeId);
        if(Objects.isNull(recipeBeforeOrder)){
            return;
        }
        recipeBeforeOrder.setIsLock(YesOrNoEnum.YES.getType());
        recipeBeforeOrderDAO.update(recipeBeforeOrder);
    }
}
