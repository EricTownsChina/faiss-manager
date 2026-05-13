package faiss.manager.config;

import com.google.common.util.concurrent.RateLimiter;
import faiss.manager.common.ErrorCode;
import faiss.manager.common.RateLimit;
import faiss.manager.common.exception.FaissManagerException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 限流拦截器
 * <p>
 * 基于 Guava RateLimiter，对标注 {@link RateLimit} 的方法进行 QPS 限流。
 * </p>
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(prefix = "faiss-manager.rate-limit", name = "enabled", havingValue = "true")
public class RateLimiterAspect {

    private final RateLimiter rateLimiter;

    public RateLimiterAspect(FaissManagerProperties properties) {
        double qps = properties.getRateLimit().getSearchQps();
        this.rateLimiter = RateLimiter.create(qps);
        log.info("Rate limiter initialized: searchQps={}", qps);
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        if (!rateLimiter.tryAcquire()) {
            log.warn("Rate limit exceeded for method: {}", joinPoint.getSignature().getName());
            throw new FaissManagerException(ErrorCode.RATE_LIMITED);
        }
        return joinPoint.proceed();
    }
}
