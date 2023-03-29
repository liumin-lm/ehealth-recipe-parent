package recipe.business;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.client.DoctorClient;
import recipe.client.IConfigurationClient;
import recipe.manager.*;

import javax.annotation.Resource;
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
    @Autowired
    protected OrganManager organManager;
    @Autowired
    protected IConfigurationClient configurationClient;
    @Autowired
    protected DoctorClient doctorClient;
    @Autowired
    protected RevisitManager revisitManager;
    @Autowired
    protected OrganDrugListManager organDrugListManager;
    @Autowired
    protected ButtonManager buttonManager;
    @Autowired
    protected PharmacyManager pharmacyManager;
    @Autowired
    protected DepartManager departManager;
    @Autowired
    protected RecipeDetailManager recipeDetailManager;
    @Resource
    protected CaManager caManager;
    @Resource
    protected DrugManager drugManager;

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());


    /**
     * callback 任务总计返回
     *
     * @param futureTasks
     * @return
     */
    protected <T> List<T> futureTaskCallbackBeanList(List<FutureTask<T>> futureTasks, Integer timeOut) {
        List<T> futureTaskCallbackBeanList = new LinkedList<>();
        long time = null == timeOut ? 10000 : timeOut;
        for (FutureTask<T> futureTask : futureTasks) {
            long start = System.currentTimeMillis();
            try {
                logger.info("BaseService futureTaskCallbackBeanList time:{}", time);
                T futureTaskCallbackBean = futureTask.get(time, TimeUnit.MILLISECONDS);
                futureTaskCallbackBeanList.add(futureTaskCallbackBean);
            } catch (Exception e) {
                logger.error("BaseService futureTaskCallbackBeanList futureTaskEnterpriseStockList error", e);
            } finally {
                Long end = System.currentTimeMillis() - start;
                time = time - end.intValue();
                if (time <= 0) {
                    time = 100;
                }
                logger.info("BaseService futureTaskCallbackBeanList timeOut：{},end:{}", time, end);
            }
        }
        logger.info("BaseService futureTaskCallbackBeanList futureTaskCallbackBeanList= {}", JSON.toJSONString(futureTaskCallbackBeanList));
        return futureTaskCallbackBeanList;
    }
}
