package recipe.factory;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 配送方式代理类
 *
 * @author fuzi
 */
@Service
public abstract class Proxy implements ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final List<Proxy> list = new ArrayList<>();


    /**
     * 获取实现类
     *
     * @param giveMode
     * @return
     */
    private void getFactoryService(Integer giveMode) {

    }

    /**
     * 添加工厂实现类
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String[] beanNames = applicationContext.getBeanNamesForType(Proxy.class);
        logger.info("GiveModeProxy添加授权服务工厂类，beanNames = {}", beanNames.toString());
        for (String beanName : beanNames) {
            Proxy giveModeService = applicationContext.getBean(beanName, Proxy.class);
            list.add(giveModeService);
        }
        logger.info("GiveModeProxy添加授权服务工厂类，giveModeMap = {}", JSON.toJSONString(list));

    }
}
