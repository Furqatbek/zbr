package com.fooddelivery.analytics.service.impl;

import com.fooddelivery.analytics.model.ActivityEventType;
import com.fooddelivery.analytics.model.ActivityLog;
import com.fooddelivery.analytics.repository.ActivityLogRepository;
import com.fooddelivery.analytics.service.ActivityTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of ActivityTrackingService.
 * Tracks user activity events asynchronously to avoid blocking the main thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityTrackingServiceImpl implements ActivityTrackingService {

    private final ActivityLogRepository activityLogRepository;

    @Override
    @Async
    @Transactional
    public ActivityLog trackActivity(
            Long userId,
            ActivityEventType eventType,
            Long restaurantId,
            Long orderId,
            String sessionId,
            String metadata) {

        log.debug("Tracking activity: userId={}, eventType={}, restaurantId={}, orderId={}",
                userId, eventType, restaurantId, orderId);

        ActivityLog activityLog = ActivityLog.builder()
                .userId(userId)
                .eventType(eventType)
                .restaurantId(restaurantId)
                .orderId(orderId)
                .sessionId(sessionId)
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .build();

        return activityLogRepository.save(activityLog);
    }

    @Override
    @Async
    @Transactional
    public ActivityLog trackActivity(Long userId, ActivityEventType eventType) {
        return trackActivity(userId, eventType, null, null, null, null);
    }

    @Override
    @Async
    @Transactional
    public ActivityLog trackRestaurantView(Long userId, Long restaurantId, String sessionId) {
        log.debug("Tracking restaurant view: userId={}, restaurantId={}", userId, restaurantId);

        ActivityLog activityLog = ActivityLog.builder()
                .userId(userId)
                .eventType(ActivityEventType.RESTAURANT_VIEW)
                .restaurantId(restaurantId)
                .sessionId(sessionId)
                .createdAt(LocalDateTime.now())
                .build();

        return activityLogRepository.save(activityLog);
    }

    @Override
    @Async
    @Transactional
    public ActivityLog trackAddToCart(Long userId, Long restaurantId, Long menuItemId, String sessionId) {
        log.debug("Tracking add to cart: userId={}, restaurantId={}, menuItemId={}",
                userId, restaurantId, menuItemId);

        ActivityLog activityLog = ActivityLog.builder()
                .userId(userId)
                .eventType(ActivityEventType.ADD_TO_CART)
                .restaurantId(restaurantId)
                .menuItemId(menuItemId)
                .sessionId(sessionId)
                .createdAt(LocalDateTime.now())
                .build();

        return activityLogRepository.save(activityLog);
    }

    @Override
    @Async
    @Transactional
    public ActivityLog trackCheckoutStart(Long userId, Long restaurantId, String sessionId) {
        log.debug("Tracking checkout start: userId={}, restaurantId={}", userId, restaurantId);

        ActivityLog activityLog = ActivityLog.builder()
                .userId(userId)
                .eventType(ActivityEventType.CHECKOUT_START)
                .restaurantId(restaurantId)
                .sessionId(sessionId)
                .createdAt(LocalDateTime.now())
                .build();

        return activityLogRepository.save(activityLog);
    }

    @Override
    @Async
    @Transactional
    public ActivityLog trackPaymentCompleted(Long userId, Long restaurantId, Long orderId, String sessionId) {
        log.debug("Tracking payment completed: userId={}, restaurantId={}, orderId={}",
                userId, restaurantId, orderId);

        ActivityLog activityLog = ActivityLog.builder()
                .userId(userId)
                .eventType(ActivityEventType.PAYMENT_COMPLETED)
                .restaurantId(restaurantId)
                .orderId(orderId)
                .sessionId(sessionId)
                .createdAt(LocalDateTime.now())
                .build();

        return activityLogRepository.save(activityLog);
    }

    @Override
    @Async
    @Transactional
    public ActivityLog trackLogin(Long userId, String deviceType, String ipAddress) {
        log.debug("Tracking login: userId={}, deviceType={}", userId, deviceType);

        ActivityLog activityLog = ActivityLog.builder()
                .userId(userId)
                .eventType(ActivityEventType.USER_LOGIN)
                .deviceType(deviceType)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();

        return activityLogRepository.save(activityLog);
    }

    @Override
    @Async
    @Transactional
    public ActivityLog trackRegistration(Long userId, String referralSource) {
        log.debug("Tracking registration: userId={}, referralSource={}", userId, referralSource);

        ActivityLog activityLog = ActivityLog.builder()
                .userId(userId)
                .eventType(ActivityEventType.USER_REGISTER)
                .referralSource(referralSource)
                .createdAt(LocalDateTime.now())
                .build();

        return activityLogRepository.save(activityLog);
    }
}
