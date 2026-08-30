package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.ConsumerAddressDto;
import com.fooddelivery.auth.dto.CreateAddressRequest;
import com.fooddelivery.auth.entity.ConsumerAddress;
import com.fooddelivery.auth.repository.ConsumerAddressRepository;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing consumer addresses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumerAddressService {

    private static final int MAX_ADDRESSES_PER_USER = 10;

    private final ConsumerAddressRepository addressRepository;

    /**
     * Get all addresses for a consumer.
     */
    @Transactional(readOnly = true)
    public List<ConsumerAddressDto> getAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Get address by ID for a consumer.
     */
    @Transactional(readOnly = true)
    public ConsumerAddressDto getAddressById(Long userId, Long addressId) {
        ConsumerAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
        return mapToDto(address);
    }

    /**
     * Add a new address for a consumer.
     */
    @Transactional
    public ConsumerAddressDto addAddress(Long userId, CreateAddressRequest request) {
        // Check max addresses limit
        long count = addressRepository.countByUserId(userId);
        if (count >= MAX_ADDRESSES_PER_USER) {
            throw new BusinessException("Maximum number of addresses (" + MAX_ADDRESSES_PER_USER + ") reached");
        }

        // If this is the first address, make it default
        boolean isFirstAddress = count == 0;

        ConsumerAddress address = ConsumerAddress.builder()
                .userId(userId)
                .label(request.getLabel())
                .fullAddress(request.getFullAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .apartmentNumber(request.getApartmentNumber())
                .entrance(request.getEntrance())
                .instructions(request.getInstructions())
                .isDefault(isFirstAddress)
                .build();

        address = addressRepository.save(address);
        log.info("Address added for user {}: {}", userId, address.getId());

        return mapToDto(address);
    }

    /**
     * Update an existing address.
     */
    @Transactional
    public ConsumerAddressDto updateAddress(Long userId, Long addressId, CreateAddressRequest request) {
        ConsumerAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        // Null means "not supplied", not "clear it" — matching updateProfile.
        // Assigning unconditionally wiped apartmentNumber, entrance and
        // instructions whenever a client sent a partial body, which is what a
        // mobile edit form typically sends. To clear a field, send "".
        if (request.getLabel() != null) {
            address.setLabel(request.getLabel());
        }
        if (request.getFullAddress() != null) {
            address.setFullAddress(request.getFullAddress());
        }
        if (request.getLatitude() != null) {
            address.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            address.setLongitude(request.getLongitude());
        }
        if (request.getApartmentNumber() != null) {
            address.setApartmentNumber(request.getApartmentNumber());
        }
        if (request.getEntrance() != null) {
            address.setEntrance(request.getEntrance());
        }
        if (request.getInstructions() != null) {
            address.setInstructions(request.getInstructions());
        }

        address = addressRepository.save(address);
        log.info("Address updated for user {}: {}", userId, addressId);

        return mapToDto(address);
    }

    /**
     * Delete an address.
     */
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        ConsumerAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);
        log.info("Address deleted for user {}: {}", userId, addressId);

        // If deleted address was default, set another address as default
        if (wasDefault) {
            List<ConsumerAddress> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                ConsumerAddress newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
                log.info("New default address set for user {}: {}", userId, newDefault.getId());
            }
        }
    }

    /**
     * Set an address as the default.
     */
    @Transactional
    public ConsumerAddressDto setDefaultAddress(Long userId, Long addressId) {
        ConsumerAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        // Reset all addresses to non-default
        addressRepository.resetDefaultForUser(userId);

        // Set this address as default
        address.setIsDefault(true);
        address = addressRepository.save(address);
        log.info("Default address set for user {}: {}", userId, addressId);

        return mapToDto(address);
    }

    /**
     * Get the default address for a consumer.
     */
    @Transactional(readOnly = true)
    public ConsumerAddressDto getDefaultAddress(Long userId) {
        return addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(this::mapToDto)
                .orElse(null);
    }

    private ConsumerAddressDto mapToDto(ConsumerAddress address) {
        return ConsumerAddressDto.builder()
                .id(address.getId())
                .label(address.getLabel())
                .fullAddress(address.getFullAddress())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .apartmentNumber(address.getApartmentNumber())
                .entrance(address.getEntrance())
                .instructions(address.getInstructions())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
