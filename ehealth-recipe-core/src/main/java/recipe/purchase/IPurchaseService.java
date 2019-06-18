package recipe.purchase;

import com.ngari.recipe.common.RecipeResultBean;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 购药方式行为
 * @version： 1.0
 */
public interface IPurchaseService {


    /**
     * 获取供应商列表
     * @param recipeId
     * @return
     */
    RecipeResultBean findSupportDepList(Integer recipeId);

    /**
     * 下单提交方法
     * @param recipeId
     * @return
     */
    RecipeResultBean order(Integer recipeId);

    
    Integer getPayMode();
}
