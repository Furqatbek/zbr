package com.fooddelivery.order.service;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.common.event.EventPublisher;
import com.fooddelivery.common.exception.BadRequestException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.order.dto.CancelOrderRequest;
import com.fooddelivery.order.dto.CreateOrderRequest;
import com.fooddelivery.order.dto.OrderDto;
import com.fooddelivery.order.dto.OrderItemRequest;
import com.fooddelivery.order.dto.UpdateOrderStatusRequest;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.entity.OrderType;
import com.fooddelivery.order.mapper.OrderMapper;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.entity.RestaurantStatus;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private User testCustomer;
    private Restaurant testRestaurant;
    private MenuItem testMenuItem;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testCustomer = User.builder()
                .id(1L)
                .email("customer@example.com")
                .firstName("Test")
                .lastName("Customer")
                .role(Role.CONSUMER)
                .build();

        testRestaurant = Restaurant.builder()
                .id(1L)
                .name("Test Restaurant")
                .slug("test-restaurant")
                .status(RestaurantStatus.ACTIVE)
                .minimumOrder(BigDecimal.TEN)
                .deliveryFee(new BigDecimal("3.99"))
                .build();

        testMenuItem = MenuItem.builder()
                .id(1L)
                .name("Test Item")
                .price(new BigDecimal("12.99"))
                .inStock(true)
                .build();

        testOrder = Order.builder()
                .id(1L)
                .externalOrderNo("ORD-2024-001")
                .consumer(testCustomer)
                .restaurant(testRestaurant)
                .status(OrderStatus.CREATED)
                .orderType(OrderType.DELIVERY)
                .subtotal(new BigDecimal("25.98"))
                .tax(new BigDecimal("2.60"))
                .deliveryFee(new BigDecimal("3.99"))
                .total(new BigDecimal("32.57"))
                .build();
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully")
        void shouldCreateOrderSuccessfully() {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .restaurantId(1L)
                    .orderType(OrderType.DELIVERY)
                    .items(List.of(
                            OrderItemRequest.builder()
                                    .menuItemId(1L)
                                    .quantity(2)
                                    .build()
                    ))
                    .deliveryAddress("123 Test St, Test City, TS 12345")
                    .build();

            OrderDto expectedDto = OrderDto.builder()
                    .id(1L)
                    .externalOrderNo("ORD-2024-001")
                    .status(OrderStatus.CREATED)
                    .build();

            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testCustomer));
            when(restaurantRepository.findById(anyLong())).thenReturn(Optional.of(testRestaurant));
            when(menuItemRepository.findById(anyLong())).thenReturn(Optional.of(testMenuItem));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(orderMapper.toDto(any(Order.class))).thenReturn(expectedDto);

            OrderDto result = orderService.createOrder(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getExternalOrderNo()).isEqualTo("ORD-2024-001");
            verify(orderRepository).save(any(Order.class));
            verify(eventPublisher).publishAsync(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should throw exception when restaurant not found")
        void shouldThrowExceptionWhenRestaurantNotFound() {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .restaurantId(999L)
                    .items(List.of())
                    .build();

            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testCustomer));
            when(restaurantRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Restaurant not found");
        }

        @Test
        @DisplayName("Should throw exception when restaurant is inactive")
        void shouldThrowExceptionWhenRestaurantInactive() {
            testRestaurant.setStatus(RestaurantStatus.SUSPENDED);
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .restaurantId(1L)
                    .items(List.of())
                    .build();

            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testCustomer));
            when(restaurantRepository.findById(anyLong())).thenReturn(Optional.of(testRestaurant));

            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Restaurant is not accepting orders");
        }
    }

    @Nested
    @DisplayName("Update Order Status Tests")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Should update order status successfully")
        void shouldUpdateOrderStatusSuccessfully() {
            OrderDto expectedDto = OrderDto.builder()
                    .id(1L)
                    .status(OrderStatus.ACCEPTED)
                    .build();

            when(orderRepository.findByIdWithLock(anyLong())).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(orderMapper.toDto(any(Order.class))).thenReturn(expectedDto);

            UpdateOrderStatusRequest statusRequest = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.ACCEPTED)
                    .build();
            OrderDto result = orderService.updateOrderStatus(1L, statusRequest);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
            verify(eventPublisher).publishAsync(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should throw exception for invalid status transition")
        void shouldThrowExceptionForInvalidStatusTransition() {
            testOrder.setStatus(OrderStatus.DELIVERED);

            when(orderRepository.findByIdWithLock(anyLong())).thenReturn(Optional.of(testOrder));

            UpdateOrderStatusRequest statusRequest = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.PREPARING)
                    .build();
            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, statusRequest))
                    .isInstanceOf(com.fooddelivery.common.exception.InvalidOperationException.class)
                    .hasMessageContaining("Cannot transition");
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderRepository.findByIdWithLock(anyLong())).thenReturn(Optional.empty());

            UpdateOrderStatusRequest statusRequest = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.ACCEPTED)
                    .build();
            assertThatThrownBy(() -> orderService.updateOrderStatus(999L, statusRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Order not found");
        }
    }

    @Nested
    @DisplayName("Cancel Order Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel order successfully")
        void shouldCancelOrderSuccessfully() {
            testOrder.setStatus(OrderStatus.CREATED);
            OrderDto expectedDto = OrderDto.builder()
                    .id(1L)
                    .status(OrderStatus.CANCELLED)
                    .build();

            when(orderRepository.findByIdWithLock(anyLong())).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(orderMapper.toDto(any(Order.class))).thenReturn(expectedDto);

            CancelOrderRequest cancelRequest = CancelOrderRequest.builder()
                    .reason("Customer requested cancellation")
                    .build();
            OrderDto result = orderService.cancelOrder(1L, cancelRequest, 1L);

            assertThat(result).isNotNull();
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw exception when trying to cancel delivered order")
        void shouldThrowExceptionWhenCancellingDeliveredOrder() {
            testOrder.setStatus(OrderStatus.DELIVERED);

            when(orderRepository.findByIdWithLock(anyLong())).thenReturn(Optional.of(testOrder));

            CancelOrderRequest cancelRequest = CancelOrderRequest.builder()
                    .reason("Too late")
                    .build();
            assertThatThrownBy(() -> orderService.cancelOrder(1L, cancelRequest, 1L))
                    .isInstanceOf(com.fooddelivery.common.exception.InvalidOperationException.class)
                    .hasMessageContaining("cannot be cancelled");
        }
    }
}
