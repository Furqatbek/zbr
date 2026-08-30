package com.fooddelivery.auth.service;

import com.fooddelivery.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records when each user was last active.
 *
 * <p>"Active" means any authenticated activity: a REST call carrying a valid
 * access token, a STOMP CONNECT, or a token refresh. That is a deliberately
 * broader definition than {@code lastLoginAt}, which only moves when
 * credentials are exchanged and therefore goes stale for a user who never logs
 * out.
 *
 * <p><b>Writes are throttled.</b> Touching a row on every request would put a
 * write — and a WAL record, and index churn — on the hot path of an endpoint
 * that otherwise only reads. At {@code app.last-seen.throttle-seconds} (default
 * 60) the recorded value is at worst that stale, which is far below the
 * granularity anyone reads it at, and the write volume drops by roughly the
 * request rate per user.
 *
 * <p>The throttle is per-instance and in memory. With more than one app
 * instance each will write once per window, which is harmless: the repository
 * update is monotonic, so the later timestamp simply wins.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LastSeenService {

    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;

    @Value("${app.last-seen.throttle-seconds:60}")
    private long throttleSeconds;

    /**
     * REQUIRES_NEW so the touch commits (or fails) on its own. Joining the
     * caller's transaction would let a rollback elsewhere silently discard it
     * and — worse — a failure here would mark the caller's transaction
     * rollback-only, turning telemetry into a broken request.
     *
     * <p>A template rather than {@code @Transactional}: the write is invoked
     * from inside this same bean, and self-invocation does not pass through the
     * proxy, so the annotation would be silently inert and the @Modifying query
     * would fail for want of a transaction.
     */
    private TransactionTemplate requiresNew;

    @PostConstruct
    void initTransactionTemplate() {
        requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Hard cap on the throttle map. Reached only with more distinct active
     * users than that in one window; the map is then dropped wholesale, which
     * costs one extra write per user and never grows without bound. A cache
     * library would be tidier but is not worth a dependency for one map.
     */
    private static final int MAX_TRACKED_USERS = 100_000;

    private final Map<Long, Long> lastWriteMillis = new ConcurrentHashMap<>();

    /**
     * Record that the given user is active right now.
     *
     * <p>Safe to call from a servlet filter or a messaging interceptor: it
     * never throws. Failing to record last-seen must not fail the request that
     * triggered it — this is telemetry, not business state.
     */
    public void touch(Long userId) {
        if (userId == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Long previous = lastWriteMillis.get(userId);
        if (previous != null && now - previous < throttleSeconds * 1000L) {
            return;
        }

        // Claim the window before writing. If two threads race, both may write
        // once; that is cheaper than holding a lock on the request path.
        if (lastWriteMillis.size() >= MAX_TRACKED_USERS) {
            lastWriteMillis.clear();
        }
        lastWriteMillis.put(userId, now);

        try {
            requiresNew.executeWithoutResult(
                    status -> userRepository.updateLastSeenAt(userId, LocalDateTime.now()));
        } catch (Exception e) {
            // Do not let a database hiccup surface as a failed API call. Debug,
            // not warn: during an outage this would fire on every request.
            log.debug("Could not record last-seen for user {}: {}", userId, e.getMessage());
        }
    }
}
