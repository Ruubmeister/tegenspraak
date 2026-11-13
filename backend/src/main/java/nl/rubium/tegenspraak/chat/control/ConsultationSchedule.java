package nl.rubium.tegenspraak.chat.control;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * When a contact answers a consultation. A random delay within the configured range makes the reply feel like a real
 * message; a failed answer is retried with a growing delay until the attempts run out.
 */
@Component
public class ConsultationSchedule {

    private final Clock clock;
    private final RandomGenerator random = new SecureRandom();
    private final ConsultationProperties properties;

    public ConsultationSchedule(Clock clock, ConsultationProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public Instant now() {
        return clock.instant();
    }

    public Instant replyDueAt(Instant requestedAt) {
        long spreadMillis =
                properties.maxReplyDelay().minus(properties.minReplyDelay()).toMillis();
        long extraMillis = spreadMillis == 0 ? 0 : random.nextLong(spreadMillis + 1);
        return requestedAt.plus(properties.minReplyDelay()).plusMillis(extraMillis);
    }

    /** Empty when the answer failed too often and should be given up. */
    public Optional<Instant> retryDueAt(int failedAttempts) {
        if (failedAttempts >= properties.maxAttempts()) {
            return Optional.empty();
        }
        return Optional.of(now().plus(properties.retryDelay().multipliedBy(failedAttempts)));
    }
}
