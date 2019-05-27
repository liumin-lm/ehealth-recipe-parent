package recipe.hisservice.syncdata;

import com.ngari.recipe.entity.Recipe;
import recipe.common.response.CommonResponse;

import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2019/2/14
 * @description： 同步监管数据服务
 * @version： 1.0
 */
public interface ICommonSyncSupervisionService {

    /**
     * 处方上传
     * @param recipeList
     * @return
     */
    CommonResponse uploadRecipeIndicators(List<Recipe> recipeList);

    /**
     * 处方核销
     * @param recipeList
     * @return
     */
    CommonResponse uploadRecipeVerificationIndicators(List<Recipe> recipeList);
}
