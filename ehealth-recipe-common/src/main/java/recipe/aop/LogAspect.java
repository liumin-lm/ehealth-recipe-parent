package recipe.aop;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author maoze
 * 这是一个日志页面
 * 对异常处理向外抛出
 */
@Aspect
//@Component(value = "logAspect")
@Slf4j
//Order值越小，优先级越高！
@Order(10)
public class LogAspect {
    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    //切点问题
    @Pointcut("!execution(* recipe.util..*.*(..)) && execution(* recipe..*.*(..))")
    public void conPoint(){}


    @Around(value = "conPoint()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = null;
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Long startTime = System.currentTimeMillis();
        try {
            Object[] objects = joinPoint.getArgs();
            logger.info("LogAspect-{} {} ,入参={}",className, methodName, JSON.toJSONString(objects));
            result =  joinPoint.proceed();
        } catch (Throwable throwable) {
            logger.error("LogAspect-{} {},Exception: {}",className,methodName ,throwable);
            throw throwable;
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info("LogAspect-{} {} ,耗时:{}ms ,出参={}",className, methodName,elapsedTime, result);
        }
        return result;
    }

}
