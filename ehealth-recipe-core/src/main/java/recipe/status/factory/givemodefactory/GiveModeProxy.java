package recipe.status.factory.givemodefactory;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import recipe.service.RecipeLogService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fuzi
 */
@Service
public class GiveModeProxy implements ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<Integer, IGiveModeService> giveModeMap = new HashMap<>();


    public void updateOrderByGiveMode(Integer giveMode, UpdateOrderStatusVO orderStatus) {
        logger.info("GiveModeProxy updateOrderByGiveMode giveMode={},orderStatus={}", giveMode, JSON.toJSONString(orderStatus));
        if (null == giveMode) {
            return;
        }
        IGiveModeService factoryService = getFactoryService(giveMode);
        //调用子类方法
        factoryService.updateStatus(orderStatus);
        //记录日志
        RecipeLogService.saveRecipeLog(orderStatus.getRecipeId(), orderStatus.getSourceRecipeOrderStatus()
                , orderStatus.getTargetRecipeOrderStatus(), "giveMode ：" + giveMode + " ,sender:" + orderStatus.getSender());
        logger.info("GiveModeProxy updateOrderByGiveMode end");

    }


    /**
     * 获取实现类
     *
     * @param giveMode
     * @return
     */
    private IGiveModeService getFactoryService(Integer giveMode) {
        IGiveModeService giveModeService = giveModeMap.get(giveMode);
        if (giveModeService == null) {
            logger.warn("GiveModeProxy无效 giveMode= {}", giveMode);
        }
        return giveModeService;
    }

    /**
     * 添加工厂实现类
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String[] beanNames = applicationContext.getBeanNamesForType(IGiveModeService.class);
        logger.info("GiveModeProxy添加授权服务工厂类，beanNames = {}", beanNames.toString());
        for (String beanName : beanNames) {
            IGiveModeService giveModeService = applicationContext.getBean(beanName, IGiveModeService.class);
            giveModeMap.put(giveModeService.getGiveMode(), giveModeService);
        }
        logger.info("GiveModeProxy添加授权服务工厂类，giveModeMap = {}", JSON.toJSONString(giveModeMap));

    }
}
