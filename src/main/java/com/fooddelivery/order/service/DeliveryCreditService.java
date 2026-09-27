package com.fooddelivery.order.service;

import com.fooddelivery.order.entity.DeliveryCredit;
import com.fooddelivery.order.repository.DeliveryCreditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Free deliveries: who is owed one, and spending it.
 *
 * <p>Two sources. Every customer gets one when they register. A referrer gets
 * one when the person they brought completes a delivery — the referred side's
 * reward is the welcome credit they already have, which is what stops one
 * person collecting two for arriving once.
 *
 * <p>Granting is idempotent by construction: both rules are partial unique
 * indexes, so a duplicate is refused by the database rather than by remembering
 * to check. Registration can be retried and a delivery event can be
 * redelivered; neither produces a second credit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryCreditService {

    private final DeliveryCreditRepository creditRepository;

    /**
     * One free delivery for a new customer.
     *
     * <p>Never throws. A credit is a nice-to-have at the end of registration and
     * must not be able to fail a sign-up — a customer who cannot create an
     * account is a worse outcome than one who has to pay for their first
     * delivery.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void grantWelcome(Long userId) {
        try {
            creditRepository.save(DeliveryCredit.builder()
                    .userId(userId)
                    .reason(DeliveryCredit.Reason.WELCOME)
                    .build());
            log.info("Welcome free delivery granted to user {}", userId);
        } catch (DataIntegrityViolationException alreadyHasOne) {
            // The unique index doing its job. Not an error: it means this
            // person already had their one.
            log.debug("User {} already has a welcome credit", userId);
        } catch (Exception e) {
            log.error("Could not grant welcome credit to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * The referrer's side: one free delivery for bringing someone who ordered.
     *
     * @return true when this call is the one that granted it
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean grantReferralReward(Long referrerUserId, Long referralId) {
        try {
            creditRepository.save(DeliveryCredit.builder()
                    .userId(referrerUserId)
                    .reason(DeliveryCredit.Reason.REFERRAL_REWARD)
                    .sourceReferralId(referralId)
                    .build());
            log.info("Referral free delivery granted to user {} for referral {}",
                    referrerUserId, referralId);
            return true;
        } catch (DataIntegrityViolationException alreadyRewarded) {
            log.debug("Referral {} has already been rewarded", referralId);
            return false;
        } catch (Exception e) {
            log.error("Could not grant referral credit to user {} for referral {}: {}",
                    referrerUserId, referralId, e.getMessage());
            return false;
        }
    }

    /**
     * Spend a credit on this order's delivery fee, if the customer has one.
     *
     * <p>Called with the order already saved, because a credit records which
     * order spent it and that is the whole audit trail.
     *
     * @return what the credit is worth on this order, or empty if there was
     *         none to spend
     */
    @Transactional
    public Optional<BigDecimal> spendOn(Long userId, Long orderId, BigDecimal deliveryFee) {
        if (deliveryFee == null || deliveryFee.compareTo(BigDecimal.ZERO) <= 0) {
            // Nothing to make free. Keep the credit for an order that costs
            // something to deliver.
            return Optional.empty();
        }

        List<DeliveryCredit> spendable = creditRepository.findSpendable(userId, LocalDateTime.now());
        if (spendable.isEmpty()) {
            return Optional.empty();
        }

        DeliveryCredit credit = spendable.get(0);
        credit.spendOn(orderId, deliveryFee);
        creditRepository.save(credit);
        log.info("Delivery credit {} ({}) spent by user {} on order {}: -{}",
                credit.getId(), credit.getReason(), userId, orderId, deliveryFee);
        return Optional.of(deliveryFee);
    }

    /** How many free deliveries this customer has waiting. */
    @Transactional(readOnly = true)
    public long available(Long userId) {
        return creditRepository.countByUserIdAndUsedAtIsNull(userId);
    }
}
