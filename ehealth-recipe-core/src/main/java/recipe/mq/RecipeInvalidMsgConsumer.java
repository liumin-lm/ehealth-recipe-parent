package recipe.mq;

import com.google.common.collect.ImmutableMap;
import com.ngari.home.asyn.model.BussCancelEvent;
import com.ngari.home.asyn.service.IAsynDoBussService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.net.broadcast.Observer;
import ctd.util.annotation.RpcBean;
import eh.cdr.constant.OrderStatusConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.BussTypeConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeMsgEnum;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.service.*;
import recipe.thread.PushRecipeToRegulationCallable;
import recipe.thread.RecipeBusiThreadPool;

import java.util.ArrayList;
import java.util.List;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * @author Created by liuxiaofeng on 2021/1/26 0026.
 *         处方失效时间非当天24点且小于24小时失效消息消费
 */
public class RecipeInvalidMsgConsumer implements Observer<String> {
    private static final Logger logger = LoggerFactory.getLogger(RecipeInvalidMsgConsumer.class);

    @Override
    public void onMessage(String msg) {
        logger.info("recipeInvalidMsgConsumer msg[{}]", msg);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(Integer.parseInt(msg));
        //过滤掉流转到扁鹊处方流转平台的处方
        if (recipe == null) {
            return;
        }
        List<Recipe> recipeList = new ArrayList<>();
        recipeList.add(recipe);
        RecipeService.doRecipeCancelByInvalidTime(recipeList);

    }

}
