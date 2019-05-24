package recipe.thread;

import ctd.util.AppContextHolder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * 线程池管理
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/14.
 */
public class RecipeBusiThreadPool {

    public static void execute(Runnable runnable) {
        ThreadPoolTaskExecutor service = AppContextHolder.getBean("busTaskExecutor", ThreadPoolTaskExecutor.class);
        if (null != service) {
            service.execute(runnable);
        }
    }


    public static void submit(Callable callable) {
        ThreadPoolTaskExecutor service = AppContextHolder.getBean("busTaskExecutor", ThreadPoolTaskExecutor.class);
        if (null != service) {
            service.submit(callable);
        }
    }

    public static void submitList(List<? extends Callable<String>> callableList) throws InterruptedException {
        ThreadPoolTaskExecutor service = AppContextHolder.getBean("busTaskExecutor", ThreadPoolTaskExecutor.class);
        if (null != service) {
            service.getThreadPoolExecutor().invokeAll(callableList);
        }
    }

}
