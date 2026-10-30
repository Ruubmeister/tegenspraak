package nl.rubium.tegenspraak.chat.control;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param minReplyDelay shortest delay before a contact answers a consultation
 * @param maxReplyDelay longest delay before a contact answers a consultation
 * @param pollInterval how often due answers are checked; read by {@link ConsultationScheduler} through its placeholder
 * @param retryDelay a failed answer is retried after this delay times the number of failures
 * @param maxAttempts how often an answer is tried in total before it is given up
 */
@Validated
@ConfigurationProperties("tegenspraak.consultations")
public record ConsultationProperties(
        @NotNull Duration minReplyDelay,
        @NotNull Duration maxReplyDelay,
        @NotNull Duration pollInterval,
        @NotNull Duration retryDelay,
        @Min(1) int maxAttempts) {

    @AssertTrue(message = "min-reply-delay must not be negative and max-reply-delay must not be below it")
    public boolean isReplyDelayRangeValid() {
        return minReplyDelay == null
                || maxReplyDelay == null
                || (!minReplyDelay.isNegative() && maxReplyDelay.compareTo(minReplyDelay) >= 0);
    }
}
