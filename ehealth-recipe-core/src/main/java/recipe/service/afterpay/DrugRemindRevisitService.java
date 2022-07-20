package recipe.service.afterpay;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.manager.RevisitManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用药提醒复诊
 */
@Component("drugRemindRevisitService")
public class DrugRemindRevisitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DrugRemindRevisitService.class);

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private RevisitManager revisitManager;

    public void drugRemind(RecipeOrder recipeOrder, List<Recipe> recipes){
        LOGGER.info("DrugRemindRevisitService drugRemind recipeOrder:{},recipes:{}", JSON.toJSONString(recipeOrder), JSON.toJSONString(recipes));
        //订单支付日期
        Date payTime = recipeOrder.getPayTime();
        List<Integer> recipeIds = recipes.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        List<RecipeExtend> recipeExtendList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIds);
        //获取长处方的处方单号
        List<Integer> longRecipeIds = recipeExtendList.stream().filter(recipeExtend -> "1".equals(recipeExtend.getIsLongRecipe())).map(RecipeExtend::getRecipeId).collect(Collectors.toList());
        List<Recipedetail> longRecipeDetailList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(longRecipeIds)) {
            longRecipeDetailList = recipeDetailDAO.findByRecipeIds(longRecipeIds);
        }
        Map<Integer, List<Recipedetail>> longRecipeDetailMap = longRecipeDetailList.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
        //获取全部的处方明细
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeIds(recipeIds);
        Map<Integer, List<Recipedetail>> recipeDetailMap = recipeDetailList.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
        recipes.forEach(recipe -> {
            List<Recipedetail> recipeDetails = recipeDetailMap.get(recipe.getRecipeId());
            //筛选出用药天数最小的日期
            Recipedetail minRecipeDetail = recipeDetails.stream().min(Comparator.comparing(Recipedetail::getUseDays)).orElse(null);
            if (null == minRecipeDetail) {
                return;
            }
            LocalDateTime payDate = Instant.ofEpochMilli(payTime.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            List<LocalDateTime> remindDates = new ArrayList<>();
            //提前3天提醒
            LocalDateTime remind3Day = payDate.plusDays(minRecipeDetail.getUseDays()).minusDays(3);
            remindDates.add(remind3Day);
            //提前2天提醒
            LocalDateTime remind2Day = payDate.plusDays(minRecipeDetail.getUseDays()).minusDays(2);
            remindDates.add(remind2Day);
            //提前1天提醒
            LocalDateTime remind1Day = payDate.plusDays(minRecipeDetail.getUseDays()).minusDays(1);
            remindDates.add(remind1Day);
            //长处方特殊提前提醒
            if (!ObjectUtils.isEmpty(longRecipeDetailMap)) {
                List<Recipedetail> longRecipeDetails = longRecipeDetailMap.get(recipe.getRecipeId());
                Recipedetail minLongRecipeDetail = longRecipeDetails.stream().min(Comparator.comparing(Recipedetail::getUseDays)).orElse(null);
                if (null == minLongRecipeDetail) {
                    return;
                }
                //长处方提前5天
                LocalDateTime remind5Day = payDate.plusDays(minLongRecipeDetail.getUseDays()).minusDays(5);
                remindDates.add(remind5Day);
                //长处方提前4天
                LocalDateTime remind4Day = payDate.plusDays(minLongRecipeDetail.getUseDays()).minusDays(4);
                remindDates.add(remind4Day);
            }
            revisitManager.remindDrugForRevisit(recipe, remindDates);
        });
    }
}
