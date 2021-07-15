package recipe.factory.status.offlineToOnlineFactory;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.vo.SettleForOfflineToOnlineVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import recipe.core.api.IOfflineToOnlineService;
import recipe.vo.patient.RecipeGiveModeButtonRes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上工厂类
 */
@Component
public class OfflineToOnlineFactory implements ApplicationContextAware, IOfflineToOnlineService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static Map<Integer, recipe.factory.status.offlineToOnlineFactory.IOfflineToOnlineService> payModeMap = new HashMap<>();

    @Override
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request) {
        recipe.factory.status.offlineToOnlineFactory.IOfflineToOnlineService offlineToOnlineService = getFactoryService(1);
        return offlineToOnlineService.settleForOfflineToOnline(request);
    }

    /**
     * 获取实现类
     *
     * @param
     * @return
     */
    public recipe.factory.status.offlineToOnlineFactory.IOfflineToOnlineService getFactoryService(Integer payMode) {
        recipe.factory.status.offlineToOnlineFactory.IOfflineToOnlineService offlineToOnlineService = payModeMap.get(payMode);
        if (offlineToOnlineService == null) {
            logger.warn("OfflineToOnlineFactory无效 payMode= {}", payMode);
        }
        return offlineToOnlineService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String[] beanNames = applicationContext.getBeanNamesForType(recipe.factory.status.offlineToOnlineFactory.IOfflineToOnlineService.class);
        logger.info("OfflineToOnlineFactory添加授权服务工厂类，beanNames = {}", beanNames.toString());
        for (String beanName : beanNames) {
            recipe.factory.status.offlineToOnlineFactory.IOfflineToOnlineService offlineToOnlineService = applicationContext.getBean(beanName, recipe.factory.status.offlineToOnlineFactory.IOfflineToOnlineService.class);
            payModeMap.put(offlineToOnlineService.getPayMode(), offlineToOnlineService);
        }
        logger.info("OfflineToOnlineFactory添加授权服务工厂类，payModeMap = {}", JSON.toJSONString(payModeMap));

    }


}
