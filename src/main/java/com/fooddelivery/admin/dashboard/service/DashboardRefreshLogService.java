package com.fooddelivery.admin.dashboard.service;

import com.fooddelivery.admin.dashboard.model.DashboardRefreshLog;
import com.fooddelivery.admin.dashboard.repository.DashboardRefreshLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for logging dashboard refresh events.
 * Uses REQUIRES_NEW propagation to run in its own transaction,
 * independent of the caller's read-only transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardRefreshLogService {

    private final DashboardRefreshLogRepository refreshLogRepository;

    /**
     * Log a dashboard refresh event in a new transaction.
     * This allows logging even when called from a read-only transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRefresh(String component, Long durationMs, boolean success, String message) {
        try {
            DashboardRefreshLog refreshLog = DashboardRefreshLog.builder()
                    .component(component)
                    .refreshedAt(LocalDateTime.now())
                    .durationMs(durationMs)
                    .success(success)
                    .message(message)
                    .build();
            refreshLogRepository.save(refreshLog);
        } catch (Exception e) {
            log.warn("Failed to log dashboard refresh: {}", e.getMessage());
        }
    }
}
