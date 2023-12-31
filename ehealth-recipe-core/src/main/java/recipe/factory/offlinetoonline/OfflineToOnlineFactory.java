package recipe.factory.offlinetoonline;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上工厂类
 */
@Component
public class OfflineToOnlineFactory implements ApplicationContextAware {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static Map<String, IOfflineToOnlineStrategy> payModeMap = new HashMap<>();

    /**
     * 获取实现类
     *
     * @param
     * @return
     */
    public IOfflineToOnlineStrategy getFactoryService(String payMode) {
        IOfflineToOnlineStrategy offlineToOnlineService = payModeMap.get(payMode);
        if (offlineToOnlineService == null) {
            logger.warn("OfflineToOnlineFactory无效 payMode= {}", payMode);
        }
        return offlineToOnlineService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String[] beanNames = applicationContext.getBeanNamesForType(IOfflineToOnlineStrategy.class);
        logger.info("OfflineToOnlineFactory添加授权服务工厂类，beanNames = {}", beanNames.toString());
        for (String beanName : beanNames) {
            IOfflineToOnlineStrategy offlineToOnlineService = applicationContext.getBean(beanName, IOfflineToOnlineStrategy.class);
            payModeMap.put(offlineToOnlineService.getHandlerMode(), offlineToOnlineService);
        }
        logger.info("OfflineToOnlineFactory添加授权服务工厂类，payModeMap = {}", JSON.toJSONString(payModeMap));
    }
}
