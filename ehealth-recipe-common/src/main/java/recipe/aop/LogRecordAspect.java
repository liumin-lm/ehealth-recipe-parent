package recipe.aop;

import com.alibaba.fastjson.JSON;
import ctd.persistence.exception.DAOException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import recipe.constant.ErrorCode;

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
public class LogRecordAspect {
    private static final Logger logger = LoggerFactory.getLogger(LogRecordAspect.class);


    @Pointcut("@annotation(recipe.aop.LogRecord)")
    public void conPoint(){}

    @Around(value = "conPoint()")
    public Object around(ProceedingJoinPoint joinPoint)  {
        Object result = null;
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();
        try {
            Object[] objects = joinPoint.getArgs();
            logger.info("LogRecordAspect-{}-{} ,入参={}", className, methodName, JSON.toJSONString(objects));
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            logger.error("LogRecordAspect-{}-{},Exception", className, methodName, throwable);
            throw new DAOException(ErrorCode.SERVICE_ERROR, throwable.getMessage());
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info("LogRecordAspect-{}-{} ,耗时:{}ms ,出参={}", className, methodName, elapsedTime, JSON.toJSONString(result));
        }
        return result;
    }

}
