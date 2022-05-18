package recipe.mq;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.RecipeExtend;
import ctd.net.broadcast.Observer;
import ctd.persistence.DAOFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.RecipeExtendDAO;

import java.util.HashMap;
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
        if (RECIPE_TYPE.equals(busType)) {
            logger.info("MqEasyPayConsumer onMessage map:{}", JSON.toJSON(map));
            if (StringUtils.isEmpty(busId)) {
                return;
            }
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(Integer.parseInt(busId));
            if (null == recipeExtend) {
                return;
            }
            if (StringUtils.isEmpty(handleType)) {
                return;
            }
            recipeExtend.setHandleType(handleType);
            recipeExtendDAO.updateNonNullFieldByPrimaryKey(recipeExtend);
        }
    }
}
