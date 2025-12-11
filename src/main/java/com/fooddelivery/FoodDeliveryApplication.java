package com.fooddelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entry point for the Food Delivery Platform.
 *
 * This is a production-ready backend for a food-delivery aggregator service
 * implementing features inspired by leading platforms like UberEats and DoorDash.
 *
 * Features:
 * - Restaurant onboarding and menu management
 * - Order processing with state machine
 * - Courier assignment and tracking
 * - Payment integration hooks
 * - Kitchen Display System (KDS) events
 * - Real-time WebSocket updates
 * - Analytics and referral programs
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class FoodDeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodDeliveryApplication.class, args);
    }
}
