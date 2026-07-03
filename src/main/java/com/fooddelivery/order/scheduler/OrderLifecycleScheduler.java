package com.fooddelivery.order.scheduler;

import com.fooddelivery.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives order-lifecycle maintenance that would otherwise never happen:
 *  - auto-cancel unpaid orders (previously implemented but never scheduled),
 *  - auto-complete delivered orders so they leave the restaurant's active list,
 *  - time out delivery orders stuck READY with no courier (cancel + refund).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderLifecycleScheduler {

    private final OrderService orderService;

    @Value("${app.order.auto-complete-delivered-minutes:60}")
    private int autoCompleteDeliveredMinutes;

    @Value("${app.order.no-courier-timeout-minutes:30}")
    private int noCourierTimeoutMinutes;

    /** Runs every 5 minutes (configurable). */
    @Scheduled(fixedDelayString = "${app.order.lifecycle-check-interval-ms:300000}")
    public void run() {
        try {
            orderService.autoCancelUnpaidOrders();
        } catch (Exception e) {
            log.error("autoCancelUnpaidOrders failed: {}", e.getMessage(), e);
        }
        try {
            orderService.autoCompleteDeliveredOrders(autoCompleteDeliveredMinutes);
        } catch (Exception e) {
            log.error("autoCompleteDeliveredOrders failed: {}", e.getMessage(), e);
        }
        try {
            orderService.cancelStuckNoCourierOrders(noCourierTimeoutMinutes);
        } catch (Exception e) {
            log.error("cancelStuckNoCourierOrders failed: {}", e.getMessage(), e);
        }
    }
}
