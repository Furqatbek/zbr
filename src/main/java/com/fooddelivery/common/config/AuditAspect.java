package com.fooddelivery.common.config;

import com.fooddelivery.common.annotation.Auditable;
import com.fooddelivery.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Aspect for audit logging of annotated methods.
 */
@Aspect
@Component
@Slf4j
public class AuditAspect {

    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String action = auditable.action();
        String entityType = auditable.entityType();
        LocalDateTime timestamp = LocalDateTime.now();

        // Get current user
        String username = getCurrentUsername();

        // Build audit log entry
        Map<String, Object> auditEntry = new HashMap<>();
        auditEntry.put("action", action);
        auditEntry.put("entityType", entityType);
        auditEntry.put("timestamp", timestamp.toString());
        auditEntry.put("user", username);
        auditEntry.put("method", joinPoint.getSignature().toShortString());

        // Log parameters if enabled
        if (auditable.logParams()) {
            Map<String, Object> params = extractParameters(joinPoint);
            auditEntry.put("parameters", params);
        }

        log.info("AUDIT_START: {}", JsonUtils.toJson(auditEntry));

        Object result;
        try {
            result = joinPoint.proceed();

            // Log result if enabled
            if (auditable.logResult() && result != null) {
                auditEntry.put("result", "SUCCESS");
                auditEntry.put("resultType", result.getClass().getSimpleName());
            }

            log.info("AUDIT_SUCCESS: action={}, user={}, entityType={}",
                    action, username, entityType);

            return result;
        } catch (Throwable e) {
            auditEntry.put("result", "FAILURE");
            auditEntry.put("error", e.getMessage());

            log.error("AUDIT_FAILURE: action={}, user={}, entityType={}, error={}",
                    action, username, entityType, e.getMessage());

            throw e;
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }

    private Map<String, Object> extractParameters(ProceedingJoinPoint joinPoint) {
        Map<String, Object> params = new HashMap<>();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length; i++) {
                // Skip sensitive parameters
                if (isSensitiveParam(paramNames[i])) {
                    params.put(paramNames[i], "[REDACTED]");
                } else if (args[i] != null) {
                    params.put(paramNames[i], sanitizeValue(args[i]));
                }
            }
        }

        return params;
    }

    private boolean isSensitiveParam(String paramName) {
        String lower = paramName.toLowerCase();
        return lower.contains("password") ||
                lower.contains("secret") ||
                lower.contains("token") ||
                lower.contains("key") ||
                lower.contains("credential");
    }

    private Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }

        // For complex objects, just return class name and toString preview
        if (!(value instanceof String ||
                value instanceof Number ||
                value instanceof Boolean)) {
            return value.getClass().getSimpleName() + ":" + truncate(value.toString(), 100);
        }

        return value;
    }

    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
