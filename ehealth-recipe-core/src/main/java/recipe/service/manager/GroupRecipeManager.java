package recipe.service.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author fuzi
 */
@Service
public class GroupRecipeManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeDAO recipeDAO;

    /**
     * 按照订单id更新同组处方状态
     *
     * @param recipe
     * @param orderId
     */
    public void updateGroupRecipe(Recipe recipe, Integer orderId) {
        logger.info("GroupRecipeManager updateGroupRecipe recipe={},orderId={}", JSON.toJSONString(recipe), orderId);
        if (null == orderId) {
            return;
        }
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderId(orderId);
        updateGroupRecipe(recipeOrder.getRecipeIdList(), recipe.getRecipeId(), recipe.getStatus());
    }

    /**
     * 按照订单code更新同组处方状态
     *
     * @param recipeId
     * @param orderCode
     * @param status
     */
    public void updateGroupRecipe(Integer recipeId, String orderCode, Integer status) {
        logger.info("GroupRecipeManager updateGroupRecipe recipe={},orderId={},status={}", recipeId, orderCode, status);
        if (null == orderCode) {
            return;
        }
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        updateGroupRecipe(recipeOrder.getRecipeIdList(), recipeId, status);
    }

    /**
     * 更新同组处方状态
     *
     * @param recipeIdListStr
     * @param outRecipeId
     * @param status
     */
    private void updateGroupRecipe(String recipeIdListStr, Integer outRecipeId, Integer status) {
        if (StringUtils.isEmpty(recipeIdListStr) || null == outRecipeId || null == status) {
            return;
        }
        List<Integer> recipeIdList = JSON.parseArray(recipeIdListStr, Integer.class);
        List<Integer> recipeIds = recipeIdList.stream().filter(a -> !a.equals(outRecipeId)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(recipeIds)) {
            return;
        }
        recipeIds.forEach(a -> {
            Recipe recipeUpdate = new Recipe();
            recipeUpdate.setStatus(status);
            recipeUpdate.setRecipeId(a);
            recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        });
    }

}
