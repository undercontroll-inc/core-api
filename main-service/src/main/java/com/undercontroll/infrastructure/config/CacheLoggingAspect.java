package com.undercontroll.infrastructure.config;

import com.undercontroll.domain.port.out.MetricsPort;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class CacheLoggingAspect {

    private final CacheManager cacheManager;
    private final MetricsPort metricsPort;

    public CacheLoggingAspect(CacheManager cacheManager, MetricsPort metricsPort) {
        this.cacheManager = cacheManager;
        this.metricsPort = metricsPort;
    }

    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object logCacheableOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Cacheable cacheable = signature.getMethod().getAnnotation(Cacheable.class);

        String[] cacheNames = cacheable.value();
        String key = generateKey(joinPoint, cacheable);
        String methodName = signature.getMethod().getName();
        String className = signature.getDeclaringType().getSimpleName();

        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Cache.ValueWrapper valueWrapper = cache.get(key);

                if (valueWrapper != null) {
                    log.debug("CACHE HIT: [{}] key='{}' method={}.{}",
                            cacheName, key, className, methodName);
                    metricsPort.incrementCacheHit(cacheName);
                    return valueWrapper.get();
                }
            }
        }

        String cacheName = cacheNames.length > 0 ? cacheNames[0] : "unknown";
        log.debug("CACHE MISS: [{}] key='{}' method={}.{}",
                cacheName, key, className, methodName);
        metricsPort.incrementCacheMiss(cacheName);

        Object result = joinPoint.proceed();

        log.debug("CACHE POPULATE: [{}] key='{}' method={}.{}",
                cacheName, key, className, methodName);

        return result;
    }

    @Around("@annotation(org.springframework.cache.annotation.CacheEvict)")
    public Object logCacheEvictOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        CacheEvict cacheEvict = signature.getMethod().getAnnotation(CacheEvict.class);

        String[] cacheNames = cacheEvict.value();
        String methodName = signature.getMethod().getName();
        String className = signature.getDeclaringType().getSimpleName();
        boolean allEntries = cacheEvict.allEntries();

        if (allEntries) {
            log.debug("CACHE EVICT ALL: caches={}", Arrays.toString(cacheNames));
            for (String cacheName : cacheNames) {
                metricsPort.incrementCacheEviction(cacheName);
            }
        } else {
            String key = generateKey(joinPoint, cacheEvict.key());
            log.debug("CACHE EVICT: caches={} key='{}' method={}.{}",
                    Arrays.toString(cacheNames), key, className, methodName);
            for (String cacheName : cacheNames) {
                metricsPort.incrementCacheEviction(cacheName);
            }
        }

        return joinPoint.proceed();
    }

    private String generateKey(ProceedingJoinPoint joinPoint, Cacheable cacheable) {
        String keyExpression = cacheable.key();
        return resolveKey(joinPoint, keyExpression);
    }

    private String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        return resolveKey(joinPoint, keyExpression);
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        if (keyExpression.isEmpty()) {
            Object[] args = joinPoint.getArgs();
            if (args.length == 0) {
                return "default";
            }
            return Arrays.toString(args);
        }

        if (keyExpression.startsWith("#")) {
            Object[] args = joinPoint.getArgs();
            String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();

            for (int i = 0; i < paramNames.length; i++) {
                if (keyExpression.contains("#" + paramNames[i])) {
                    return String.valueOf(args[i]);
                }
            }
        }

        return keyExpression;
    }
}
