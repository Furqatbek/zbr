package com.fooddelivery.common.config;

import com.fooddelivery.common.annotation.RateLimited;
import com.fooddelivery.common.util.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect for applying rate limiting to annotated methods.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final RateLimitService rateLimitService;

    @Around("@annotation(rateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String key = resolveKey(joinPoint, rateLimited);

        rateLimitService.checkRateLimit(
                key,
                rateLimited.requestsPerMinute(),
                rateLimited.burstCapacity()
        );

        return joinPoint.proceed();
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, RateLimited rateLimited) {
        switch (rateLimited.keyType()) {
            case USER:
                return resolveUserKey();
            case ENDPOINT:
                return resolveEndpointKey(joinPoint);
            case IP:
            default:
                return resolveIpKey();
        }
    }

    private String resolveIpKey() {
        String ip = getClientIp();
        return RateLimitService.ipKey(ip);
    }

    private String resolveUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal())) {
            return RateLimitService.userKey(getUserId(authentication));
        }
        // Fall back to IP if not authenticated
        return resolveIpKey();
    }

    private String resolveEndpointKey(ProceedingJoinPoint joinPoint) {
        String endpoint = joinPoint.getSignature().toShortString();
        String identifier = resolveIpKey();
        return RateLimitService.endpointKey(endpoint, identifier);
    }

    private String getClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();

        // X-Real-IP FIRST, deliberately. nginx overwrites it with a single
        // authoritative value, whereas X-Forwarded-For is a client-supplied
        // list that a misconfigured proxy may merely append to — trusting its
        // leftmost entry lets a caller choose its own rate-limit bucket. Both
        // headers are only meaningful because nothing but nginx can reach this
        // app (all other ports are bound to 127.0.0.1).
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp.trim();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private Long getUserId(Authentication authentication) {
        // Assuming the principal contains user ID
        // This would depend on your UserDetails implementation
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.fooddelivery.auth.security.UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        return 0L;
    }
}
