package recipe.service;

import com.google.common.collect.Maps;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.RecipeDAO;
import recipe.drugsenterprise.ThirdEnterpriseCallService;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;
import recipe.util.DateConversion;
import recipe.util.RedisClient;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 电子处方定时任务服务
 * @author yuyun
 */
@RpcBean(value = "recipeTimedTaskService")
public class RecipeTimedTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeTimedTaskService.class);

    private static final String HIS_RECIPE_KEY_PREFIX =  "hisRecipe_";

    @Autowired
    private RemoteRecipeService remoteRecipeService;


    /**
     * 定时任务 钥匙圈处方 配送中状态 持续一周后系统自动完成该笔业务
     */
    @RpcService
    public void autoFinishRecipeTask() {
        String endDt =
                DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(RecipeSystemConstant.ONE_WEEK_AGO),
                        DateConversion.DEFAULT_DATE_TIME);
        String startDt =
                DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(
                        RecipeSystemConstant.ONE_MONTH_AGO), DateConversion.DEFAULT_DATE_TIME);

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Recipe> recipes = recipeDAO.findNotConfirmReceiptList(startDt, endDt);

        ThirdEnterpriseCallService service = ApplicationUtils.getRecipeService(
                ThirdEnterpriseCallService.class, "takeDrugService");

        if (null != recipes && recipes.size() > 0) {
            for (Recipe recipe : recipes) {
                Map<String, Object> paramMap = Maps.newHashMap();
                paramMap.put("recipeId", recipe.getRecipeId());
                paramMap.put("sender", "systemTask");
                service.finishRecipe(paramMap);
            }
        }
        LOGGER.info("autoFinishRecipeTask size={}", null == recipes ? "null" : recipes.size());

    }

    /**
     * 定时任务 每天12:10点定时将redis里的处方和处方详情保存到数据库
     */
    @RpcService
    public void autoSaveRecipeByRedis(){
        RedisClient redisClient = RedisClient.instance();
        RecipeBean recipeBean;
        List<RecipeDetailBean> recipeDetailBeans;
        Map<String,Object> objectMap;
        //遍历redis里带有hisRecipe_前缀的所有keys
        Set<String> keys = null;
        try {
            keys = redisClient.scan(HIS_RECIPE_KEY_PREFIX+"*");
        } catch (Exception e) {
            LOGGER.error("redis error" + e.toString());
        }
        //取出每一个key对应的map
        Map<String, Object> map;
        if (null != keys && keys.size() > 0) {
            for (String key : keys) {
                try {
                    map = redisClient.hScan(key,10000,"*");
                } catch (Exception e) {
                    LOGGER.error("redis error" + e.toString());
                    continue;
                }
                //遍历map取出value
                for (Map.Entry<String,Object> entry : map.entrySet()){

                    objectMap =(Map<String,Object>)entry.getValue();
                    //取到需要保存处方单和处方详情，save到数据库
                    recipeBean = (RecipeBean) objectMap.get("recipeBean");
                    recipeDetailBeans = (List<RecipeDetailBean>) objectMap.get("recipeDetailBeans");
                    boolean flag = true;
                    try {
                        remoteRecipeService.saveRecipeDataFromPayment(recipeBean, recipeDetailBeans);
                    }catch (Exception e) {
                        LOGGER.error("recipeService.saveRecipeDataFromPayment error" + e.toString());
                        flag = false;
                    }finally {
                        //删除redis key
                        if (flag){
                            redisClient.del(key);
                        }

                    }
                }

            }
        }

    }
}
