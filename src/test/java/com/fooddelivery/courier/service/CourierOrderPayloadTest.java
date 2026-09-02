package com.fooddelivery.courier.service;

import com.fooddelivery.analytics.financial.service.CommissionService;
import com.fooddelivery.auth.service.UserService;
import com.fooddelivery.common.event.EventPublisher;
import com.fooddelivery.courier.dto.CourierOrderDto;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.entity.CourierStatus;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.order.repository.DeliveryIssueRepository;
import com.fooddelivery.notification.service.PersistentNotificationService;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.repository.PaymentRepository;
import com.fooddelivery.restaurant.entity.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * What a courier can see about an order they are being offered.
 *
 * <p>The pickup call refuses with "Order is not ready for pickup yet" until the
 * restaurant marks the food ready, so a courier standing in the shop needs to
 * reach the KITCHEN. The payload carried only the customer's number, which is
 * no use for that — reported by the courier app team.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CourierOrderDto payload")
class CourierOrderPayloadTest {

    @Mock private CourierRepository courierRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private DeliveryIssueRepository deliveryIssueRepository;
    @Mock private UserService userService;
    @Mock private EventPublisher eventPublisher;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private PersistentNotificationService notificationService;
    @Mock private CommissionService commissionService;

    @InjectMocks
    private CourierService courierService;

    @Test
    @DisplayName("carries the restaurant's phone, distinct from the customer's")
    void carriesRestaurantPhone() {
        Restaurant restaurant = Restaurant.builder()
                .id(3L).name("Osh Markazi")
                .phone("998712001122")
                .addressLine1("Amir Temur 1").city("Toshkent")
                .latitude(new BigDecimal("41.3110810"))
                .longitude(new BigDecimal("69.2405620"))
                .build();

        Order order = Order.builder()
                .id(77L)
                .restaurant(restaurant)
                .status(OrderStatus.READY)
                .customerName("Asad Karimov")
                .customerPhone("998901234567")
                .deliveryFee(new BigDecimal("15000"))
                .build();

        Courier courier = Courier.builder()
                .id(9L).status(CourierStatus.AVAILABLE).verified(true)
                .build();

        when(courierRepository.findByIdWithUser(9L)).thenReturn(Optional.of(courier));
        when(orderRepository.findAvailableOrdersForCourier()).thenReturn(List.of(order));

        List<CourierOrderDto> offered = courierService.getAvailableOrders(9L);

        assertThat(offered).hasSize(1);
        CourierOrderDto dto = offered.get(0);
        assertThat(dto.getRestaurantPhone()).isEqualTo("998712001122");
        // The two numbers must not be confused for one another.
        assertThat(dto.getCustomerPhone()).isEqualTo("998901234567");
        assertThat(dto.getRestaurantPhone()).isNotEqualTo(dto.getCustomerPhone());
    }
}
