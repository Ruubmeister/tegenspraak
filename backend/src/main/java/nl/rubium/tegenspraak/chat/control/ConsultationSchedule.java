package nl.rubium.tegenspraak.chat.control;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * When a contact answers a consultation. A random delay within the configured range makes the reply feel like a real
 * message; a failed answer is retried with a growing delay until the attempts run out.
 */
@Component
public class ConsultationSchedule {

    private final Clock clock;
    private final RandomGenerator random = new SecureRandom();
    private final Duration minReplyDelay;
    private final Duration maxReplyDelay;
    private final Duration retryDelay;
    private final int maxAttempts;

    public ConsultationSchedule(
            Clock clock,
            @Value("${tegenspraak.consultations.min-reply-delay}") Duration minReplyDelay,
            @Value("${tegenspraak.consultations.max-reply-delay}") Duration maxReplyDelay,
            @Value("${tegenspraak.consultations.retry-delay}") Duration retryDelay,
            @Value("${tegenspraak.consultations.max-attempts}") int maxAttempts) {
        if (minReplyDelay.isNegative() || maxReplyDelay.compareTo(minReplyDelay) < 0) {
            throw new IllegalArgumentException("The reply delay range of consultations is invalid");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Consultations need at least one attempt");
        }
        this.clock = clock;
        this.minReplyDelay = minReplyDelay;
        this.maxReplyDelay = maxReplyDelay;
        this.retryDelay = retryDelay;
        this.maxAttempts = maxAttempts;
    }

    public Instant now() {
        return clock.instant();
    }

    public Instant replyDueAt(Instant requestedAt) {
        long spreadMillis = maxReplyDelay.minus(minReplyDelay).toMillis();
        long extraMillis = spreadMillis == 0 ? 0 : random.nextLong(spreadMillis + 1);
        return requestedAt.plus(minReplyDelay).plusMillis(extraMillis);
    }

    /** Empty when the answer failed too often and should be given up. */
    public Optional<Instant> retryDueAt(int failedAttempts) {
        if (failedAttempts >= maxAttempts) {
            return Optional.empty();
        }
        return Optional.of(now().plus(retryDelay.multipliedBy(failedAttempts)));
    }
}
