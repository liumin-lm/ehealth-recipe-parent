package recipe.mq;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.net.broadcast.Observer;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;

import java.util.List;
import java.util.Map;

/**
 * 接收业务办理类型
 */
public class MqEasyPayConsumer implements Observer<Map<String,String>> {

    private static final String RECIPE_TYPE = "recipe";

    private static final Logger logger = LoggerFactory.getLogger(MqEasyPayConsumer.class);
    @Override
    public void onMessage(Map<String,String> map) {
        String busType = map.get("busType");
        String busId = map.get("busId");
        String handleType = map.get("handleType");
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        if (RECIPE_TYPE.equals(busType)) {
            logger.info("MqEasyPayConsumer onMessage map:{}", JSON.toJSON(map));
            if (StringUtils.isEmpty(busId)) {
                return;
            }
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderId(Integer.parseInt(busId));
            if (null == recipeOrder) {
                return;
            }
            List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
            recipeIdList.forEach(recipeId ->{
                try {
                    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
                    recipeExtend.setHandleType(handleType);
                    recipeExtendDAO.updateNonNullFieldByPrimaryKey(recipeExtend);
                } catch (NumberFormatException e) {
                    logger.error("MqEasyPayConsumer onMessage recipeId:{} error", recipeId, e);
                }
            });
        }
    }
}
