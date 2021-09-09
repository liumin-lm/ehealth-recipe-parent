package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.recipe.dto.GroupRecipeConfDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.IConfigurationClient;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 同组处方处理
 *
 * @author fuzi
 */
@Service
public class GroupRecipeManager extends BaseManager {
    @Autowired
    private ICurrentUserInfoService currentUserInfoService;
    @Autowired
    private IConfigurationClient configurationClient;

    /**
     * 按照订单id更新同组处方状态
     *
     * @param recipe  处方
     * @param orderId 机构id
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
     * @param recipeId  处方id
     * @param orderCode 订单编号
     * @param status    修改目标处方状态
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
     * 获取机构是否合并支付的配制
     * 和配制的合并支付的方式（挂号序号合并还是同一挂号序号相同病种）
     *
     * @return 合并支付的配制项
     */
    public GroupRecipeConfDTO getMergeRecipeSetting() {
        List<Integer> organIds = currentUserInfoService.getCurrentOrganIds();
        GroupRecipeConfDTO result = new GroupRecipeConfDTO(false, "e.registerId");
        logger.info("GroupRecipeManager getMergeRecipeSetting organIds={}", JSON.toJSONString(organIds));
        if (CollectionUtils.isEmpty(organIds)) {
            return result;
        }
        Boolean mergeRecipeFlag = organIds.stream().allMatch(a -> configurationClient.getValueBooleanCatch(a, "mergeRecipeFlag", false));

        result.setMergeRecipeFlag(mergeRecipeFlag);
        if (!mergeRecipeFlag) {
            return result;
        }

        Set<String> set = new HashSet<>();
        for (Integer a : organIds) {
            String mergeRecipeWay = configurationClient.getValueCatch(a, "mergeRecipeWay", "e.registerId");
            set.add(mergeRecipeWay);
            if (set.size() > 1) {
                result.setMergeRecipeFlag(false);
                result.setMergeRecipeWayAfter(mergeRecipeWay);
                break;
            }
            result.setMergeRecipeWayAfter(mergeRecipeWay);
        }
        logger.info("GroupRecipeManager getMergeRecipeSetting result={}", JSON.toJSONString(result));
        return result;
    }

    /**
     * 更新同组处方状态
     *
     * @param recipeIdListStr 处方id
     * @param outRecipeId     不修改的处方id
     * @param status          修改目标处方状态
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
