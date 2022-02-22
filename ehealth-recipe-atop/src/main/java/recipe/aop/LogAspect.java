package recipe.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;

/**
 * @author maoze
 * 这是一个日志页面
 * 对异常处理向外抛出
 */
@Aspect
@Slf4j
@Order(10)
public class LogAspect {

    /**
     * 切面
     */
    @Pointcut("execution(* recipe.atop..*.*(..))")
    public void conPoint() {
    }


    @Around(value = "conPoint()")
    public Object around(ProceedingJoinPoint joinPoint) {
        return LogRecordAspect.aroundStatic(joinPoint);
    }

}
