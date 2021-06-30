package recipe.service.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;

import java.util.List;

/**
 * 处方订单
 * @author yinsheng
 * @date 2021\6\30 0030 14:21
 */
@Service
public class RecipeManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RecipeOrderDAO recipeOrderDAO;

    @Autowired
    private RecipeDAO recipeDAO;

    /**
     * 通过订单号获取该订单下关联的所有处方
     * @param orderCode 订单号
     * @return          处方集合
     */
    public List<Recipe> getRecipesByOrderCode(String orderCode) {
        logger.info("RecipeOrderManager getRecipesByOrderCode orderCode:{}.", orderCode);
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIdList);
        logger.info("RecipeOrderManager getRecipesByOrderCode recipes:{}.", JSON.toJSONString(recipes));
        return recipes;
    }
}
