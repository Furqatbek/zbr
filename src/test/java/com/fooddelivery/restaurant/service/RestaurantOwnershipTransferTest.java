package com.fooddelivery.restaurant.service;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.auth.service.UserService;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.service.ImageStorageService;
import com.fooddelivery.restaurant.dto.RestaurantDto;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.mapper.RestaurantMapper;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Transferring a restaurant is irreversible from the previous owner's side —
 * they lose access the moment it commits — so the guards around it matter more
 * than the happy path.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RestaurantService.transferOwnership")
class RestaurantOwnershipTransferTest {

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private RestaurantMapper restaurantMapper;
    @Mock
    private UserService userService;
    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private RestaurantService restaurantService;

    private Restaurant restaurant;
    private User previousOwner;

    private User user(Long id, UserStatus status) {
        return User.builder().id(id).email("u" + id + "@example.com").status(status).build();
    }

    @BeforeEach
    void setUp() {
        previousOwner = user(1L, UserStatus.ACTIVE);
        restaurant = Restaurant.builder().id(10L).name("Osh Markazi").owner(previousOwner).build();

        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(i -> i.getArgument(0));
        when(restaurantMapper.toDto(any(Restaurant.class))).thenReturn(new RestaurantDto());
    }

    @Test
    @DisplayName("moves the restaurant and grants the new owner the role")
    void transfersAndGrantsRole() {
        User newOwner = user(2L, UserStatus.ACTIVE);
        when(userService.getUserEntityById(2L)).thenReturn(newOwner);

        restaurantService.transferOwnership(10L, 2L);

        assertThat(restaurant.getOwner()).isSameAs(newOwner);
        assertThat(newOwner.hasRole(Role.RESTAURANT_OWNER)).isTrue();
    }

    @Test
    @DisplayName("leaves the previous owner's role alone — they may own others")
    void doesNotRevokePreviousOwnersRole() {
        previousOwner.addRole(Role.RESTAURANT_OWNER);
        User newOwner = user(2L, UserStatus.ACTIVE);
        when(userService.getUserEntityById(2L)).thenReturn(newOwner);

        restaurantService.transferOwnership(10L, 2L);

        assertThat(previousOwner.hasRole(Role.RESTAURANT_OWNER)).isTrue();
    }

    @Test
    @DisplayName("refuses to strand a restaurant on a suspended account")
    void rejectsNonActiveTarget() {
        when(userService.getUserEntityById(2L)).thenReturn(user(2L, UserStatus.SUSPENDED));

        assertThatThrownBy(() -> restaurantService.transferOwnership(10L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SUSPENDED");

        assertThat(restaurant.getOwner()).isSameAs(previousOwner);
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    @DisplayName("a repeated transfer is a no-op, not an error")
    void isIdempotent() {
        restaurantService.transferOwnership(10L, 1L);

        assertThat(restaurant.getOwner()).isSameAs(previousOwner);
        // Nothing to write, and crucially no pointless audit entry.
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    @DisplayName("an unknown target user fails before anything is written")
    void rejectsUnknownUser() {
        when(userService.getUserEntityById(99L))
                .thenThrow(new ResourceNotFoundException("User", "id", 99L));

        assertThatThrownBy(() -> restaurantService.transferOwnership(10L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(restaurant.getOwner()).isSameAs(previousOwner);
        verify(restaurantRepository, never()).save(any());
    }
}
