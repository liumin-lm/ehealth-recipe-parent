package recipe.thread;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.service.RecipeHisService;

import java.util.List;
import java.util.Map;

/**
 * 更新从HIS获取到的处方状态
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/15.
 */
public class UpdateRecipeStatusFromHisCallable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRecipeStatusFromHisCallable.class);

    private Map<Integer, List<String>> map;

    public UpdateRecipeStatusFromHisCallable(Map<Integer, List<String>> map) {
        this.map = map;
    }

    @Override
    public void run() {
        //HIS消息发送
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        for (Integer organId : map.keySet()) {
            try {
                hisService.recipeListQuery(map.get(organId), organId);
            } catch (Exception e) {
                LOGGER.error("UpdateRecipeStatusFromHisCallable map:{}, error", JSON.toJSONString(map), e);
            }
        }
    }
}
