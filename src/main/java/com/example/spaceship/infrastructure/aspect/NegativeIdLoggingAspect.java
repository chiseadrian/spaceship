package com.example.spaceship.infrastructure.aspect;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class NegativeIdLoggingAspect {

    @Around("execution(* com.example.spaceship.application.service.ShipService.getShipById(..)) && args(id)")
    public Object logNegativeId(ProceedingJoinPoint joinPoint, Long id) throws Throwable {
        if (id != null && id < 0) {
            log.warn("A ship with negative ID has been requested: {}", id);
        }
        return joinPoint.proceed();
    }
}