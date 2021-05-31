package recipe.service.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.grouprecipe.model.GroupRecipeConf;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
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
 * 同组处方处理
 *
 * @author fuzi
 */
@Service
public class GroupRecipeManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private IConfigurationCenterUtilsService configService;

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

    /**
     * 获取机构是否合并支付的配制
     * 和配制的合并支付的方式（挂号序号合并还是同一挂号序号相同病种）
     *
     * @return 合并支付的配制项
     */
    public GroupRecipeConf getMergeRecipeSetting(){
        GroupRecipeConf result = new GroupRecipeConf();
        //默认
        Boolean mergeRecipeFlag = false;
        String mergeRecipeWayAfter = "e.registerId";
        try {
            //获取是否合并处方的配置--区域公众号如果有一个没开就默认全部关闭
            ICurrentUserInfoService currentUserInfoService = AppDomainContext.getBean("eh.remoteCurrentUserInfoService", ICurrentUserInfoService.class);
            List<Integer> organIds = currentUserInfoService.getCurrentOrganIds();
            logger.info("MergeRecipeManager organIds={}", JSONUtils.toString(organIds));
            if (CollectionUtils.isNotEmpty(organIds)) {
                for (Integer organId : organIds) {
                    //TODO 配制项获取的修改
                    //获取区域公众号
                    mergeRecipeFlag = (Boolean) configService.getConfiguration(organId, "mergeRecipeFlag");
                    if (mergeRecipeFlag == null || !mergeRecipeFlag) {
                        mergeRecipeFlag = false;
                        break;
                    }
                }
            }
            //再根据区域公众号里是否都支持同一种合并方式
            if (mergeRecipeFlag) {
                //TODO 配制项获取的修改
                //获取合并处方分组方式
                //e.registerId支持同一个挂号序号下的处方合并支付
                //e.registerId,e.chronicDiseaseName支持同一个挂号序号且同一个病种的处方合并支付
                String mergeRecipeWay = (String) configService.getConfiguration(organIds.get(0), "mergeRecipeWay");
                //默认挂号序号分组
                if (StringUtils.isEmpty(mergeRecipeWay)) {
                    mergeRecipeWay = "e.registerId";
                }
                //如果只有一个就取第一个
                if (organIds.size() == 1) {
                    mergeRecipeWayAfter = mergeRecipeWay;
                }
                //从第二个开始进行比较
                for (Integer organId : organIds) {
                    mergeRecipeWayAfter = (String) configService.getConfiguration(organId, "mergeRecipeWay");
                    if (!mergeRecipeWay.equals(mergeRecipeWayAfter)) {
                        mergeRecipeFlag = false;
                        logger.info("MergeRecipeManager 区域公众号存在机构配置不一致:organId={},mergeRecipeWay={}", organId, mergeRecipeWay);
                        break;
                    }
                }
                logger.info("MergeRecipeManager mergeRecipeFlag={},mergeRecipeWay={}", mergeRecipeFlag, mergeRecipeWay);
            }
        } catch (Exception e) {
            logger.error("MergeRecipeManager error configService", e);
        }
        result.setMergeRecipeFlag(mergeRecipeFlag);
        result.setMergeRecipeWayAfter(mergeRecipeWayAfter);
        logger.info("MergeRecipeManager result={}", JSONUtils.toString(result));
        return result;
    }

}
