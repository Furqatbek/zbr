package com.fooddelivery.auth.service;

import com.fooddelivery.auth.repository.ConsumerAddressRepository;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.notification.repository.UserDeviceTokenRepository;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The phone half of the admin user search. The repository query is exercised
 * against a real database in UserSearchQueryTest; what is checked here is the
 * value the service derives and hands it, since getting that wrong is silent —
 * it returns plausible-looking results rather than an error.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService.searchUsers — phone term derivation")
class UserSearchPhoneTermTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserDeviceTokenRepository deviceTokenRepository;
    @Mock private ConsumerAddressRepository consumerAddressRepository;
    @Mock private CourierRepository courierRepository;
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private UserService userService;

    /**
     * The second argument the service passes to the repository.
     *
     * <p>Resets the mock first so each call is independently verifiable —
     * without it, a test asserting on several queries fails on the second,
     * because the single-invocation verify below sees the accumulated calls.
     */
    private String phoneTermFor(String query) {
        org.mockito.Mockito.reset(userRepository);
        when(userRepository.searchUsers(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(Page.empty());

        userService.searchUsers(query, PageRequest.of(0, 20));

        ArgumentCaptor<String> search = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> phone = ArgumentCaptor.forClass(String.class);
        verify(userRepository).searchUsers(search.capture(), phone.capture(), any(Pageable.class));

        // The name/email term must always be the untouched query — stripping
        // punctuation out of a name would corrupt it.
        assertThat(search.getValue()).isEqualTo(query);
        return phone.getValue();
    }

    @Test
    @DisplayName("a punctuated phone number is reduced to digits")
    void reducesPunctuatedPhone() {
        assertThat(phoneTermFor("+998 90 123 45 67")).isEqualTo("998901234567");
        assertThat(phoneTermFor("+998901234567")).isEqualTo("998901234567");
        assertThat(phoneTermFor("90-123-45-67")).isEqualTo("901234567");
    }

    @Test
    @DisplayName("an email is passed through whole, not reduced to its digits")
    void doesNotReduceAnEmail() {
        // "user1@example.com" reduced to "1" would match nearly every phone on
        // the platform and bury the one exact match the operator wanted.
        assertThat(phoneTermFor("user1@example.com")).isEqualTo("user1@example.com");
    }

    @Test
    @DisplayName("a name is passed through whole")
    void doesNotReduceAName() {
        assertThat(phoneTermFor("Asad Karimov")).isEqualTo("Asad Karimov");
    }

    @Test
    @DisplayName("a blank query stays blank rather than becoming a wildcard")
    void blankStaysBlank() {
        assertThat(phoneTermFor("")).isEmpty();
    }

    @Test
    @DisplayName("results are still mapped through")
    void mapsResults() {
        when(userRepository.searchUsers(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(Page.empty());
        var result = userService.searchUsers("asad", PageRequest.of(0, 20));
        assertThat(result.getContent()).isEmpty();
        assertThat(result.isEmpty()).isTrue();
    }
}
