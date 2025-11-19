package com.a3solutions.fsm.common;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.common
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.a3solutions.fsm..controller..*(..)) || " +
            "execution(* com.a3solutions.fsm..service..*(..)) || " +
            "execution(* com.a3solutions.fsm.technician.*Service.*(..)) || " +
            "execution(* com.a3solutions.fsm.workorder.*Service.*(..)) || " +
            "execution(* com.a3solutions.fsm.auth.AuthService.*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        log.info("Entering {}", method);

        try {
            Object result = joinPoint.proceed();
            long time = System.currentTimeMillis() - start;
            log.info("Exiting {} ({} ms)", method, time);
            return result;
        } catch (Exception ex) {
            long time = System.currentTimeMillis() - start;
            log.error("Exception in {} after {} ms: {}", method, time, ex.getMessage());
            throw ex;
        }
    }
}
