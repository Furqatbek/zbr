package com.fooddelivery.order.mapper;

import com.fooddelivery.common.util.JsonUtils;
import com.fooddelivery.order.dto.OrderDto;
import com.fooddelivery.order.dto.OrderItemDto;
import com.fooddelivery.order.dto.PaymentDto;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderItem;
import com.fooddelivery.order.entity.Payment;
import org.mapstruct.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for Order entities and DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "consumerId", source = "consumer.id")
    @Mapping(target = "consumerName", source = "consumer.fullName")
    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "restaurantName", source = "restaurant.name")
    @Mapping(target = "courierId", source = "courier.id")
    @Mapping(target = "courierName", expression = "java(order.getCourier() != null ? order.getCourier().getUser().getFullName() : null)")
    @Mapping(target = "courierPhone", expression = "java(order.getCourier() != null ? order.getCourier().getUser().getPhone() : null)")
    OrderDto toDto(Order order);

    List<OrderDto> toDtoList(List<Order> orders);

    @Mapping(target = "menuItemId", source = "menuItem.id")
    @Mapping(target = "modifiers", expression = "java(parseModifiers(orderItem.getModifiersJson()))")
    OrderItemDto toItemDto(OrderItem orderItem);

    List<OrderItemDto> toItemDtoList(List<OrderItem> orderItems);

    @Mapping(target = "orderId", source = "order.id")
    PaymentDto toPaymentDto(Payment payment);

    List<PaymentDto> toPaymentDtoList(List<Payment> payments);

    @SuppressWarnings("unchecked")
    default List<OrderItemDto.ModifierDto> parseModifiers(String modifiersJson) {
        if (modifiersJson == null || modifiersJson.isBlank()) {
            return null;
        }
        try {
            List<Map> rawModifiers = JsonUtils.fromJsonToList(modifiersJson, Map.class);
            return rawModifiers.stream()
                    .map(m -> OrderItemDto.ModifierDto.builder()
                            .id(((Number) m.get("id")).longValue())
                            .name((String) m.get("name"))
                            .price(new java.math.BigDecimal(m.get("price").toString()))
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }
}
