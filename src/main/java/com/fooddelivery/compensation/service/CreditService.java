package com.fooddelivery.compensation.service;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.compensation.entity.CustomerCredit;
import com.fooddelivery.compensation.repository.CustomerCreditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for managing customer credits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditService {

    private final CustomerCreditRepository creditRepository;
    private final UserRepository userRepository;

    /**
     * Get or create credit record for a user.
     */
    @Transactional
    public CustomerCredit getOrCreateCredit(Long userId) {
        return creditRepository.findByUserId(userId)
                .orElseGet(() -> createCreditForUser(userId));
    }

    /**
     * Add credit to a user's account.
     */
    @Transactional
    public CustomerCredit addCredit(Long userId, BigDecimal amount) {
        CustomerCredit credit = creditRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> createCreditForUser(userId));

        credit.addCredit(amount);
        CustomerCredit saved = creditRepository.save(credit);

        log.info("Added {} credit to user {}. New balance: {}",
                amount, userId, saved.getBalance());

        return saved;
    }

    /**
     * Use credit from a user's account.
     */
    @Transactional
    public boolean useCredit(Long userId, BigDecimal amount) {
        CustomerCredit credit = creditRepository.findByUserIdWithLock(userId)
                .orElse(null);

        if (credit == null || !credit.hasSufficientCredit(amount)) {
            log.warn("Insufficient credit for user {}. Requested: {}, Available: {}",
                    userId, amount, credit != null ? credit.getBalance() : BigDecimal.ZERO);
            return false;
        }

        credit.useCredit(amount);
        creditRepository.save(credit);

        log.info("Used {} credit from user {}. New balance: {}",
                amount, userId, credit.getBalance());

        return true;
    }

    /**
     * Get current credit balance for a user.
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        return creditRepository.findByUserId(userId)
                .map(CustomerCredit::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Check if user has sufficient credit.
     */
    @Transactional(readOnly = true)
    public boolean hasSufficientCredit(Long userId, BigDecimal amount) {
        return creditRepository.findByUserId(userId)
                .map(credit -> credit.hasSufficientCredit(amount))
                .orElse(false);
    }

    /**
     * Create a new credit record for a user.
     */
    private CustomerCredit createCreditForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        CustomerCredit credit = CustomerCredit.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .lifetimeEarned(BigDecimal.ZERO)
                .lifetimeUsed(BigDecimal.ZERO)
                .build();

        return creditRepository.save(credit);
    }
}
