package recipe.status.factory.recipestatusfactory;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fuzi
 */
@Service
public class RecipeOrderStatusProxy implements ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<Integer, IRecipeOrderStatusService> recipeOrderStatusMap = new HashMap<>();

    public Recipe updateOrderByStatus(UpdateOrderStatusVO orderStatus) {
        Integer status = orderStatus.getSourceRecipeOrderStatus();
        if (null == status) {
            return null;
        }
        IRecipeOrderStatusService factoryService = getFactoryService(status);
        //调用子类方法
        return factoryService.updateStatus(orderStatus);

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
            logger.warn("RecipeOrderStatusProxy无效 giveMode= {}", status);
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
