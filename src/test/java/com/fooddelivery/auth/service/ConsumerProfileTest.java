package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.CreateAddressRequest;
import com.fooddelivery.auth.dto.UpdateProfileRequest;
import com.fooddelivery.auth.dto.UserDto;
import com.fooddelivery.auth.entity.ConsumerAddress;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.repository.ConsumerAddressRepository;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.security.JwtService;
import com.fooddelivery.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Regressions for the customer app's "profile and address changes do not
 * persist" report. Each case here silently returned 200 while writing nothing,
 * or wrote nulls over fields the client never sent — the worst shape a bug can
 * take, because the client had no way to detect it.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Consumer profile and address persistence")
class ConsumerProfileTest {

    @Mock
    private OtpService otpService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private ConsumerAddressService addressService;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PhoneAuthService phoneAuthService;

    @Mock
    private ConsumerAddressRepository addressRepository;

    @InjectMocks
    private ConsumerAddressService realAddressService;

    private User consumer() {
        return User.builder()
                .id(7L)
                .phone("998901234567")
                .firstName("Old")
                .lastName("Name")
                .role(Role.CONSUMER)
                .build();
    }

    private void stubUser(User user) {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.countByConsumerId(anyLong())).thenReturn(3L);
    }

    @Nested
    @DisplayName("PUT /consumers/profile")
    class UpdateProfile {

        @Test
        @DisplayName("fullName is applied — it used to be dropped as an unknown property")
        void fullNameIsApplied() {
            User user = consumer();
            stubUser(user);

            UserDto result = phoneAuthService.updateProfile(
                    7L, UpdateProfileRequest.builder().fullName("Asad Karimov").build());

            assertThat(user.getFirstName()).isEqualTo("Asad");
            assertThat(user.getLastName()).isEqualTo("Karimov");
            assertThat(result.getFullName()).isEqualTo("Asad Karimov");
        }

        @Test
        @DisplayName("a one-word fullName clears the surname rather than leaving the old one")
        void singleWordFullName() {
            User user = consumer();
            stubUser(user);

            phoneAuthService.updateProfile(
                    7L, UpdateProfileRequest.builder().fullName("Asad").build());

            assertThat(user.getFirstName()).isEqualTo("Asad");
            assertThat(user.getLastName()).isEmpty();
        }

        @Test
        @DisplayName("an explicit firstName still wins over fullName in the same request")
        void explicitNameWins() {
            User user = consumer();
            stubUser(user);

            phoneAuthService.updateProfile(7L, UpdateProfileRequest.builder()
                    .fullName("Asad Karimov")
                    .firstName("Aziz")
                    .build());

            assertThat(user.getFirstName()).isEqualTo("Aziz");
            assertThat(user.getLastName()).isEqualTo("Karimov");
        }

        @Test
        @DisplayName("the response carries the whole profile, not a subset")
        void responseIsTheFullProfile() {
            User user = consumer();
            user.setProfileImageUrl("https://cdn.example/avatar.png");
            stubUser(user);

            UserDto result = phoneAuthService.updateProfile(
                    7L, UpdateProfileRequest.builder().fullName("Asad Karimov").build());

            assertThat(result.getId()).isEqualTo(7L);
            assertThat(result.getPhone()).isEqualTo("998901234567");
            assertThat(result.getAvatarUrl()).isEqualTo("https://cdn.example/avatar.png");
            assertThat(result.getTotalOrders()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("PUT /consumers/addresses/{id}")
    class UpdateAddress {

        @Test
        @DisplayName("omitted optional fields are left alone, not nulled")
        void partialUpdateDoesNotWipeFields() {
            ConsumerAddress existing = ConsumerAddress.builder()
                    .id(3L)
                    .userId(7L)
                    .label("Home")
                    .fullAddress("Amir Temur 1")
                    .apartmentNumber("42")
                    .entrance("2")
                    .instructions("Ring twice")
                    .isDefault(true)
                    .build();

            when(addressRepository.findByIdAndUserId(3L, 7L)).thenReturn(Optional.of(existing));
            when(addressRepository.save(any(ConsumerAddress.class))).thenAnswer(i -> i.getArgument(0));

            // The client is only moving the pin; it sends nothing else.
            realAddressService.updateAddress(7L, 3L, CreateAddressRequest.builder()
                    .label("Home")
                    .fullAddress("Amir Temur 2")
                    .build());

            assertThat(existing.getFullAddress()).isEqualTo("Amir Temur 2");
            assertThat(existing.getApartmentNumber()).isEqualTo("42");
            assertThat(existing.getEntrance()).isEqualTo("2");
            assertThat(existing.getInstructions()).isEqualTo("Ring twice");
        }

        @Test
        @DisplayName("an empty string still clears a field — that is how a client erases one")
        void emptyStringClears() {
            ConsumerAddress existing = ConsumerAddress.builder()
                    .id(3L)
                    .userId(7L)
                    .label("Home")
                    .fullAddress("Amir Temur 1")
                    .instructions("Ring twice")
                    .build();

            when(addressRepository.findByIdAndUserId(3L, 7L)).thenReturn(Optional.of(existing));
            when(addressRepository.save(any(ConsumerAddress.class))).thenAnswer(i -> i.getArgument(0));

            realAddressService.updateAddress(7L, 3L, CreateAddressRequest.builder()
                    .label("Home")
                    .fullAddress("Amir Temur 1")
                    .instructions("")
                    .build());

            assertThat(existing.getInstructions()).isEmpty();
        }
    }
}
