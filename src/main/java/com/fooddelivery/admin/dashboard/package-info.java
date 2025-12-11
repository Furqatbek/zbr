/**
 * Admin Dashboard Module for Food Delivery Platform.
 *
 * <h2>Overview</h2>
 * <p>This module provides real-time operational insights for administrators,
 * covering orders, restaurants, couriers, finance, and customer support.</p>
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@code controller} - REST API endpoints for dashboard operations</li>
 *   <li>{@code service} - Business logic and dashboard data aggregation</li>
 *   <li>{@code repository} - Data access layer with optimized queries</li>
 *   <li>{@code collector} - Data collectors for aggregating metrics</li>
 *   <li>{@code model} - JPA entities for dashboard data</li>
 *   <li>{@code dto} - Data transfer objects for API responses</li>
 *   <li>{@code mapper} - MapStruct mappers for entity-DTO conversion</li>
 *   <li>{@code util} - Utility classes including metrics calculators</li>
 *   <li>{@code config} - Configuration classes including Redis cache</li>
 * </ul>
 *
 * <h2>Dashboard Panels</h2>
 * <ol>
 *   <li><b>Overview</b> - Key metrics: orders, revenue, active entities, system status</li>
 *   <li><b>Orders</b> - Active orders, stuck orders, cancelled, rejected</li>
 *   <li><b>Restaurants</b> - Online/offline status, performance, acceptance rates</li>
 *   <li><b>Couriers</b> - Locations, availability, performance metrics</li>
 *   <li><b>Finance</b> - GMV, commission, payouts, refunds, discounts</li>
 *   <li><b>Support</b> - Tickets, resolution times, agent performance</li>
 * </ol>
 *
 * <h2>Caching Strategy</h2>
 * <ul>
 *   <li>Overview: 15 seconds TTL</li>
 *   <li>Active orders: 5 seconds TTL</li>
 *   <li>Stuck orders: 10 seconds TTL</li>
 *   <li>Restaurant metrics: 30 seconds TTL</li>
 *   <li>Courier metrics: 15 seconds TTL</li>
 *   <li>Finance metrics: 30 seconds TTL</li>
 *   <li>Support metrics: 30 seconds TTL</li>
 * </ul>
 *
 * <h2>API Endpoints</h2>
 * <p>Base path: {@code /api/v1/admin/dashboard}</p>
 * <ul>
 *   <li>{@code GET /overview} - Dashboard overview</li>
 *   <li>{@code GET|POST /orders/active} - Active orders</li>
 *   <li>{@code GET|POST /orders/stuck} - Stuck orders</li>
 *   <li>{@code POST /orders/cancelled} - Cancelled orders</li>
 *   <li>{@code POST /orders/rejected} - Rejected orders</li>
 *   <li>{@code GET|POST /restaurants} - Restaurant metrics</li>
 *   <li>{@code GET|POST /couriers} - Courier metrics</li>
 *   <li>{@code GET|POST /finance} - Finance metrics</li>
 *   <li>{@code GET|POST /support} - Support metrics</li>
 *   <li>{@code POST /cache/refresh} - Refresh caches (admin only)</li>
 * </ul>
 *
 * @see com.fooddelivery.admin.dashboard.service.AdminDashboardService
 * @see com.fooddelivery.admin.dashboard.controller.AdminDashboardController
 * @since 1.0
 */
package com.fooddelivery.admin.dashboard;
