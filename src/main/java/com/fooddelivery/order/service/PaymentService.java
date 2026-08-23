package com.fooddelivery.order.service;

import com.fooddelivery.common.annotation.Auditable;
import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.common.event.EventPublisher;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.PaymentException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.util.SlugUtils;
import com.fooddelivery.order.dto.CreatePaymentRequest;
import com.fooddelivery.order.dto.PaymentDto;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.Payment;
import com.fooddelivery.order.entity.PaymentStatus;
import com.fooddelivery.order.event.PaymentConfirmedEvent;
import com.fooddelivery.order.mapper.OrderMapper;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for payment operations.
 * Provides hooks for Stripe-like payment provider integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final EventPublisher eventPublisher;

    @Value("${app.payment.provider:stripe}")
    private String paymentProvider;

    @Value("${app.currency:UZS}")
    private String currency = "UZS";

    /**
     * Create a payment intent for an order.
     * In production, this would integrate with Stripe or similar provider.
     */
    @Transactional
    @Auditable(action = "CREATE_PAYMENT", entityType = "Payment")
    public PaymentDto createPaymentIntent(Long orderId, CreatePaymentRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Check for existing payment
        paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.CONFIRMED)
                .ifPresent(p -> {
                    throw new BusinessException("Order already has a confirmed payment");
                });

        // Create payment record
        String paymentIntentId = "pi_" + SlugUtils.generateRandomCode(24);

        Payment payment = Payment.builder()
                .order(order)
                .provider(paymentProvider)
                .paymentIntentId(paymentIntentId)
                .paymentMethod(request.getPaymentMethod())
                .amount(order.getTotal())
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment intent created for order {}: {}", order.getExternalOrderNo(), paymentIntentId);

        // In production, you would call Stripe API here:
        // PaymentIntent intent = PaymentIntent.create(params);
        // payment.setPaymentIntentId(intent.getId());

        PaymentDto dto = orderMapper.toPaymentDto(payment);
        // Client secret would come from Stripe in production
        dto.setClientSecret("sk_test_" + SlugUtils.generateRandomCode(32));

        return dto;
    }

    /**
     * Confirm payment (called after successful payment on client side or via webhook).
     */
    @Transactional
    @Auditable(action = "CONFIRM_PAYMENT", entityType = "Payment")
    public PaymentDto confirmPayment(String paymentIntentId, String providerPaymentId, String rawResponse) {
        Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentIntentId", paymentIntentId));

        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            return orderMapper.toPaymentDto(payment);
        }

        if (!payment.getStatus().canTransitionTo(PaymentStatus.CONFIRMED)) {
            throw new PaymentException("Cannot confirm payment in status: " + payment.getStatus());
        }

        payment.markAsConfirmed(providerPaymentId, rawResponse);
        payment = paymentRepository.save(payment);

        // Update order payment status
        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.CONFIRMED);
        orderRepository.save(order);

        log.info("Payment confirmed for order {}: {}", order.getExternalOrderNo(), providerPaymentId);

        // Publish event
        publishPaymentConfirmedEvent(payment, order);

        return orderMapper.toPaymentDto(payment);
    }

    /**
     * Handle payment failure.
     */
    @Transactional
    @Auditable(action = "FAIL_PAYMENT", entityType = "Payment")
    public PaymentDto failPayment(String paymentIntentId, String errorCode, String errorMessage, String rawResponse) {
        Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentIntentId", paymentIntentId));

        payment.markAsFailed(errorCode, errorMessage, rawResponse);
        payment = paymentRepository.save(payment);

        // Update order payment status
        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.FAILED);
        orderRepository.save(order);

        log.warn("Payment failed for order {}: {} - {}", order.getExternalOrderNo(), errorCode, errorMessage);

        return orderMapper.toPaymentDto(payment);
    }

    /**
     * Process refund.
     */
    @Transactional
    @Auditable(action = "REFUND_PAYMENT", entityType = "Payment")
    public PaymentDto refundPayment(Long orderId, BigDecimal refundAmount, String reason) {
        Payment payment = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.CONFIRMED)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        if (!payment.getStatus().isRefundable()) {
            throw new PaymentException("Payment cannot be refunded in status: " + payment.getStatus());
        }

        if (refundAmount == null) {
            refundAmount = payment.getAmount();
        }

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new PaymentException("Refund amount cannot exceed payment amount");
        }

        // In production, call Stripe refund API here
        // Refund refund = Refund.create(params);

        payment.markAsRefunded(refundAmount, reason);
        payment = paymentRepository.save(payment);

        // Update order payment status
        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(order);

        log.info("Payment refunded for order {}: {} (reason: {})",
                order.getExternalOrderNo(), refundAmount, reason);

        return orderMapper.toPaymentDto(payment);
    }

    /**
     * Get payment by order ID.
     */
    @Transactional(readOnly = true)
    public PaymentDto getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
        return orderMapper.toPaymentDto(payment);
    }

    private void publishPaymentConfirmedEvent(Payment payment, Order order) {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                payment.getId(),
                order.getId(),
                order.getExternalOrderNo(),
                order.getConsumer().getId(),
                order.getRestaurant().getId(),
                payment.getAmount(),
                payment.getProvider(),
                payment.getProviderPaymentId()
        );
        eventPublisher.publishAsync(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_CONFIRMED_KEY,
                event
        );
    }
}
