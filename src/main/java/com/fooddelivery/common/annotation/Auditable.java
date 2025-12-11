package com.fooddelivery.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for audit logging.
 * Automatically logs the action, user, timestamp, and parameters.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * The action being performed (e.g., "CREATE_ORDER", "UPDATE_STATUS").
     */
    String action();

    /**
     * The entity type being operated on (e.g., "Order", "Restaurant").
     */
    String entityType() default "";

    /**
     * Whether to log method parameters.
     */
    boolean logParams() default true;

    /**
     * Whether to log the return value.
     */
    boolean logResult() default false;
}
