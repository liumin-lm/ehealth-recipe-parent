package recipe.service.afterpay;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import recipe.client.IConfigurationClient;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.enumerate.type.RecipeTypeEnum;
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
    @Autowired
    private IConfigurationClient configurationClient;

    public void drugRemind(RecipeOrder recipeOrder, List<Recipe> recipes) {
        LOGGER.info("DrugRemindRevisitService drugRemind recipeOrder:{},recipes:{}", JSON.toJSONString(recipeOrder), JSON.toJSONString(recipes));
        List<String> revisitRementAppointDepart =new ArrayList<>();
        LocalDateTime revisitRemindLocalDate=null;
        Date revisitRemindDate=recipeOrder.getRevisitRemindTime();
        if (revisitRemindDate != null) {
            revisitRemindLocalDate=Instant.ofEpochMilli(revisitRemindDate.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }

        //订单支付日期
        Date payTime = recipeOrder.getPayTime();
        List<Integer> recipeIds = recipes.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        List<Recipe> tcmRecipeList = recipes.stream().filter(recipe -> RecipeTypeEnum.RECIPETYPE_TCM.getType().equals(recipe.getRecipeType())).collect(Collectors.toList());
        List<RecipeExtend> recipeExtendList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIds);
        //获取长处方的处方单号
        List<Integer> longRecipeIds = recipeExtendList.stream().filter(recipeExtend -> "1".equals(recipeExtend.getIsLongRecipe())).map(RecipeExtend::getRecipeId).collect(Collectors.toList());
        //获取全部的处方明细
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeIds(recipeIds);
        LOGGER.info("DrugRemindRevisitService drugRemind recipeDetailList:{}", JSON.toJSONString(recipeDetailList));
        if (CollectionUtils.isNotEmpty(tcmRecipeList)) {
            //中药处方 1帖=1天
            for (Recipe tcmRecipe : tcmRecipeList) {
                for (Recipedetail recipeDetail : recipeDetailList) {
                    if (tcmRecipe.getRecipeId().equals(recipeDetail.getRecipeId())) {
                        recipeDetail.setUseDays(tcmRecipe.getCopyNum());
                    }
                }
            }
            LOGGER.info("DrugRemindRevisitService drugRemind convert recipeDetailList:{}", JSON.toJSONString(recipeDetailList));
        }
        Map<Integer, List<Recipedetail>> recipeDetailMap = recipeDetailList.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
        String config = configurationClient.getValueCatch(recipes.get(0).getClinicOrgan(), "revisitRemindNotify", "");
        if(StringUtils.isNotEmpty(config)){
            revisitRementAppointDepart=Arrays.asList(config.split(","));
        }
        List<String> finalRevisitRementAppointDepart = revisitRementAppointDepart;
        LocalDateTime finalRevisitRemindLocalDate = revisitRemindLocalDate;
        recipes.forEach(recipe -> {
            if(CollectionUtils.isNotEmpty(finalRevisitRementAppointDepart) && !finalRevisitRementAppointDepart.contains(recipe.getAppointDepart())){
                return;
            }
            List<Recipedetail> recipeDetails = recipeDetailMap.get(recipe.getRecipeId());
            //筛选出用药天数大于4天的最小日期
            recipeDetails = recipeDetails.stream().filter(x -> x.getUseDays() > 4).collect(Collectors.toList());
            Recipedetail minRecipeDetail = recipeDetails.stream().min(Comparator.comparing(Recipedetail::getUseDays)).orElse(null);
            if (null == minRecipeDetail) {
                return;
            }
            LocalDateTime payDate = Instant.ofEpochMilli(payTime.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            List<LocalDateTime> remindDates = new ArrayList<>();
            //三种推送时间方案，按订单号除以3取余
            int pushMode = recipeOrder.getOrderId() % 3 + 1;
            switch (pushMode) {
                case 1:
                    //方案一：长处方提前1天和3天， 非长处方提前1天
                    remindDates.add(payDate.plusDays(minRecipeDetail.getUseDays()).minusDays(1));
                    if (longRecipeIds.contains(recipe.getRecipeId())) {
                        remindDates.add(payDate.plusDays(minRecipeDetail.getUseDays()).minusDays(3));
                    }
                    break;
                case 2:
                    //方案二：长处方提前2天和4天， 非长处方提前2天
                    remindDates.add(payDate.plusDays(minRecipeDetail.getUseDays()).minusDays(2));
                    if (longRecipeIds.contains(recipe.getRecipeId())) {
                        remindDates.add(payDate.plusDays(minRecipeDetail.getUseDays()).minusDays(4));
                    }
                    break;
                case 3:
                    //方案三：长处方提前3天和5天， 非长处方提前3天
                    remindDates.add(payDate.plusDays(minRecipeDetail.getUseDays()).minusDays(3));
                    if (longRecipeIds.contains(recipe.getRecipeId())) {
                        remindDates.add(payDate.plusDays(minRecipeDetail.getUseDays()).minusDays(5));
                    }
                    break;
                default:
                    break;
            }
            //患者修改提醒时间，以修改时间为准进行提醒
            if(null!=finalRevisitRemindLocalDate){
                remindDates.remove(remindDates.get(0));
                remindDates.add(finalRevisitRemindLocalDate);
            }
            revisitManager.remindDrugForRevisit(recipe, remindDates, pushMode);
        });
    }
}
