package recipe.business;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * 业务核心逻辑处理 基类
 *
 * @author fuzi
 */
public class BaseService {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());


    /**
     * callback 任务总计返回
     *
     * @param futureTasks
     * @return
     */
    protected <T> List<T> futureTaskCallbackBeanList(List<FutureTask<T>> futureTasks) {
        List<T> futureTaskCallbackBeanList = new LinkedList<>();
        for (FutureTask<T> futureTask : futureTasks) {
            try {
                T futureTaskCallbackBean = futureTask.get(8000, TimeUnit.MILLISECONDS);
                futureTaskCallbackBeanList.add(futureTaskCallbackBean);
            } catch (Exception e) {
                logger.error("BaseService futureTaskCallbackBeanList futureTaskEnterpriseStockList error", e);
            }
        }
        logger.info("BaseService futureTaskCallbackBeanList futureTaskCallbackBeanList= {}", JSON.toJSONString(futureTaskCallbackBeanList));
        return futureTaskCallbackBeanList;
    }

}
