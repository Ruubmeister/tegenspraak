package nl.rubium.tegenspraak.investigation.control;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param maxWrongAttempts wrong codes a team may enter per locked target within the window before it has to wait
 * @param wrongAttemptWindow the sliding window in which wrong codes are counted
 */
@Validated
@ConfigurationProperties("tegenspraak.locks")
public record LockProperties(
        @Positive int maxWrongAttempts, @NotNull Duration wrongAttemptWindow) {}
