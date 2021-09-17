package recipe.aop;

import ctd.util.JSONUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 处方日志AOP切面 目前用于线程池任务计时
 * 给需要切点加上LogInfo注解即可
 */
@Aspect
public class RecipeLogAspect {

    private final Logger logger = LoggerFactory.getLogger(RecipeLogAspect.class);

    @Pointcut("@annotation(recipe.aop.LogInfo)")
    private void logPointCut(){}

    @Around("logPointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint){
        Object result = null;
        //获取类名
        String className = joinPoint.getTarget().getClass().getSimpleName();
        //获取方法名
        String methodName = joinPoint.getSignature().getName();
        //获取参数
        String paramString = getParam(joinPoint);
        logger.info("{} {},parameter:{}.", className, methodName, paramString);
        long start = 0;
        try {
            start = System.currentTimeMillis();
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            logger.error("RecipeLogAspect doAround error", throwable);
        } finally {
            long elapsedTime = System.currentTimeMillis() - start;
            logger.info("{} {} 执行时间:{}ms.", className, methodName, elapsedTime);
        }
        return result;
    }

    private String getParam(JoinPoint joinPoint) {
        Object[] paramValues = joinPoint.getArgs();
        return JSONUtils.toString(paramValues);
    }
}
