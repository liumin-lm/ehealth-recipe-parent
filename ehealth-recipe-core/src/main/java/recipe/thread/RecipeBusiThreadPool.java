package recipe.thread;

import ctd.util.AppContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池管理
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/14.
 */
public class RecipeBusiThreadPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeBusiThreadPool.class);

    public static void execute(Runnable runnable) {
        ThreadPoolTaskExecutor service = AppContextHolder.getBean("busTaskExecutor", ThreadPoolTaskExecutor.class);
        printThreadPoolInfo(service);
        if (null != service) {
            service.execute(runnable);
        }
    }


    public static void submit(Callable callable) {
        ThreadPoolTaskExecutor service = AppContextHolder.getBean("busTaskExecutor", ThreadPoolTaskExecutor.class);
        printThreadPoolInfo(service);
        if (null != service) {
            service.submit(callable);
        }
    }

    public static void submitList(List<? extends Callable<String>> callableList) throws InterruptedException {
        ThreadPoolTaskExecutor service = AppContextHolder.getBean("busTaskExecutor", ThreadPoolTaskExecutor.class);
        printThreadPoolInfo(service);
        if (null != service) {
            service.getThreadPoolExecutor().invokeAll(callableList);
        }
    }

    /**
     * 打印当前线程池工作状态
     * @param service
     */
    private static void printThreadPoolInfo(ThreadPoolTaskExecutor service){
        try {
            if (null != service) {
                ThreadPoolExecutor threadPoolExecutor = service.getThreadPoolExecutor();
                if (null != threadPoolExecutor && threadPoolExecutor.getQueue().size() > 10) {
                    LOGGER.info("RecipeBusiThreadPool printThreadPoolInfo 当前线程池排队线程数:{},当前线程池活动线程数:{},当前线程池完成线程数:{},当前线程池总线程数:{}.", threadPoolExecutor.getQueue().size(), threadPoolExecutor.getActiveCount(), threadPoolExecutor.getCompletedTaskCount(), threadPoolExecutor.getTaskCount());
                }
            }
        } catch (IllegalStateException e) {
            LOGGER.error("RecipeBusiThreadPool printThreadPoolInfo error: ", e);
        }
    }

}
