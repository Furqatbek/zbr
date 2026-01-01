package com.fooddelivery.order.entity;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order entity representing customer orders.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_consumer", columnList = "consumer_id"),
        @Index(name = "idx_orders_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_orders_courier", columnList = "courier_id"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_external_no", columnList = "external_order_no"),
        @Index(name = "idx_orders_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_order_no", nullable = false, unique = true, length = 50)
    private String externalOrderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumer_id", nullable = false)
    private User consumer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private Courier courier;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // Order items
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    // Pricing
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "delivery_fee", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "tip_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tipAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    // Delivery details
    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_latitude", precision = 10, scale = 7)
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude", precision = 10, scale = 7)
    private BigDecimal deliveryLongitude;

    @Column(name = "delivery_instructions", length = 500)
    private String deliveryInstructions;

    // For dine-in orders
    @Column(name = "table_id")
    private String tableId;

    // Contact info
    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    // Notes and special requests
    @Column(length = 1000)
    private String notes;

    // Timing
    @Column(name = "estimated_prep_time_minutes")
    private Integer estimatedPrepTimeMinutes;

    @Column(name = "estimated_delivery_time")
    private LocalDateTime estimatedDeliveryTime;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "preparing_at")
    private LocalDateTime preparingAt;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // Rating
    @Column(name = "consumer_rating")
    private Integer consumerRating;

    @Column(name = "consumer_review", length = 1000)
    private String consumerReview;

    // Concurrency control
    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    public void calculateTotals() {
        this.subtotal = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.total = subtotal
                .add(tax != null ? tax : BigDecimal.ZERO)
                .add(deliveryFee != null ? deliveryFee : BigDecimal.ZERO)
                .add(tipAmount != null ? tipAmount : BigDecimal.ZERO)
                .subtract(discount != null ? discount : BigDecimal.ZERO);
    }

    public boolean canTransitionTo(OrderStatus newStatus) {
        return status.canTransitionTo(newStatus);
    }

    public boolean isCancellable() {
        return status.isCancellable();
    }

    public void updateStatus(OrderStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from " + status + " to " + newStatus);
        }

        this.status = newStatus;

        // Update timestamps
        switch (newStatus) {
            case ACCEPTED -> this.acceptedAt = LocalDateTime.now();
            case PREPARING -> this.preparingAt = LocalDateTime.now();
            case READY -> this.readyAt = LocalDateTime.now();
            case PICKED_UP -> this.pickedUpAt = LocalDateTime.now();
            case DELIVERED -> this.deliveredAt = LocalDateTime.now();
            case COMPLETED -> this.completedAt = LocalDateTime.now();
            case CANCELLED -> this.cancelledAt = LocalDateTime.now();
            default -> {}
        }
    }
}
