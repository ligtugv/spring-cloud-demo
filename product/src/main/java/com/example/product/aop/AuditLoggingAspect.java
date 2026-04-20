package com.example.product.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class AuditLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingAspect.class);

    @Pointcut("execution(* com.example.product.controller..*(..))")
    public void controllerLayer() {}

    @Before("controllerLayer()")
    public void logBefore(JoinPoint joinPoint) {
        String method = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String params = Arrays.stream(args)
                .filter(a -> !(a instanceof org.springframework.web.multipart.MultipartFile))
                .map(a -> a == null ? "null" : a.toString())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);
        log.info("[AUDIT] --> {} | params=[{}]", method, params);
    }

    @AfterReturning(pointcut = "controllerLayer()", returning = "result")
    public void logAfter(JoinPoint joinPoint, Object result) {
        String method = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        String summary = "null";
        if (result instanceof org.springframework.http.ResponseEntity<?> resp) {
            summary = String.valueOf(resp.getStatusCode().value());
        } else if (result instanceof Iterable<?> it) {
            int count = 0;
            for (Object ignored : it) count++;
            summary = "List[size=" + count + "]";
        } else if (result != null) {
            summary = result.getClass().getSimpleName();
        }
        log.info("[AUDIT] <-- {} | result={}", method, summary);
    }
}
