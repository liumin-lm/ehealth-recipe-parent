package recipe.factory.status.orderstatusfactory;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单状态修改代理类
 *
 * @author fuzi
 */
@Service
public class RecipeOrderStatusProxy implements ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<Integer, IRecipeOrderStatusService> recipeOrderStatusMap = new HashMap<>();

    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeDAO recipeDAO;

    /**
     * 根据订单状态 更新处方状态
     *
     * @param orderStatus
     * @return
     */
    public Recipe updateOrderByStatus(UpdateOrderStatusVO orderStatus, RecipeOrder recipeOrder, Recipe recipe) {
        logger.info("RecipeOrderStatusProxy updateOrderByStatus orderStatus = {}", JSON.toJSONString(orderStatus));
        Integer status = orderStatus.getTargetRecipeOrderStatus();
        if (null == status) {
            return null;
        }
        IRecipeOrderStatusService factoryService = getFactoryService(status);
        //根据订单状态 更新处方状态
        factoryService.updateStatus(orderStatus, recipeOrder, recipe);
        orderStatus.setTargetRecipeStatus(recipe.getStatus());
        //更新处方状态
        recipeDAO.updateNonNullFieldByPrimaryKey(recipe);
        //更新订单状态
        recipeOrder.setStatus(orderStatus.getTargetRecipeOrderStatus());
        //订单状态改变时间
        recipeOrder.setDispensingStatusAlterTime(new Date());
        recipeOrderDAO.updateNonNullFieldByPrimaryKey(recipeOrder);
        //异步处方信息上传
        factoryService.upRecipeThreadPool(recipe);
        //更新同组处方状态
        factoryService.updateGroupRecipe(recipe, recipeOrder.getOrderId());
        logger.info("RecipeOrderStatusProxy updateOrderByStatus recipe = {}", JSON.toJSONString(recipe));
        return recipe;
    }

    /**
     * 获取实现类
     *
     * @param status
     * @return
     */
    private IRecipeOrderStatusService getFactoryService(Integer status) {
        IRecipeOrderStatusService recipeOrderStatusService = recipeOrderStatusMap.get(status);
        if (recipeOrderStatusService == null) {
            logger.warn("RecipeOrderStatusProxy无效 status= {}", status);
        }
        return recipeOrderStatusService;
    }

    /**
     * 添加工厂实现类
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String[] beanNames = applicationContext.getBeanNamesForType(IRecipeOrderStatusService.class);
        logger.info("RecipeOrderStatusProxy添加授权服务工厂类，beanNames = {}", beanNames.toString());
        for (String beanName : beanNames) {
            IRecipeOrderStatusService recipeOrderStatusService = applicationContext.getBean(beanName, IRecipeOrderStatusService.class);
            recipeOrderStatusMap.put(recipeOrderStatusService.getStatus(), recipeOrderStatusService);
        }
        logger.info("RecipeOrderStatusProxy添加授权服务工厂类，recipeOrderStatusMap = {}", JSON.toJSONString(recipeOrderStatusMap));

    }


}
