package com.fooddelivery.restaurant.mapper;

import com.fooddelivery.restaurant.dto.*;
import com.fooddelivery.restaurant.entity.*;
import org.mapstruct.*;

import java.util.Collection;
import java.util.List;

/**
 * MapStruct mapper for Restaurant entities and DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RestaurantMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "fullAddress", expression = "java(restaurant.getFullAddress())")
    @Mapping(target = "isCurrentlyOpen", expression = "java(restaurant.isCurrentlyOpen())")
    RestaurantDto toDto(Restaurant restaurant);

    List<RestaurantDto> toDtoList(List<Restaurant> restaurants);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "coverImageUrl", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "featured", ignore = true)
    @Mapping(target = "commissionRate", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalRatings", ignore = true)
    @Mapping(target = "totalOrders", ignore = true)
    @Mapping(target = "isOpen", ignore = true)
    @Mapping(target = "configJson", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Restaurant toEntity(CreateRestaurantRequest request);

    @Mapping(target = "restaurantId", source = "restaurant.id")
    MenuCategoryDto toCategoryDto(MenuCategory category);

    List<MenuCategoryDto> toCategoryDtoList(List<MenuCategory> categories);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MenuCategory toEntity(CreateMenuCategoryRequest request);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "effectivePrice", expression = "java(item.getEffectivePrice())")
    @Mapping(target = "onSale", expression = "java(item.isOnSale())")
    @Mapping(target = "discountPercentage", expression = "java(item.getDiscountPercentage())")
    MenuItemDto toItemDto(MenuItem item);

    List<MenuItemDto> toItemDtoList(List<MenuItem> items);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "priceWithMargin", ignore = true)
    @Mapping(target = "inStock", constant = "true")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "imagePath", ignore = true)
    @Mapping(target = "imageName", ignore = true)
    @Mapping(target = "imageSize", ignore = true)
    @Mapping(target = "imageContentType", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MenuItem toEntity(CreateMenuItemRequest request);

    @Mapping(target = "menuItemId", source = "menuItem.id")
    @Mapping(target = "totalPrice", expression = "java(variant.calculateTotalPrice())")
    ItemVariantDto toVariantDto(ItemVariant variant);

    List<ItemVariantDto> toVariantDtoList(Collection<ItemVariant> variants);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "menuItem", ignore = true)
    @Mapping(target = "inStock", constant = "true")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    ItemVariant toEntity(CreateItemVariantRequest request);

    @Mapping(target = "menuItemId", source = "menuItem.id")
    ItemOptionDto toOptionDto(ItemOption option);

    List<ItemOptionDto> toOptionDtoList(Collection<ItemOption> options);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "menuItem", ignore = true)
    @Mapping(target = "inStock", constant = "true")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    ItemOption toEntity(CreateItemOptionRequest request);
}
