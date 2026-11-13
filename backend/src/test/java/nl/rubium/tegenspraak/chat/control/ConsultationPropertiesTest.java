package nl.rubium.tegenspraak.chat.control;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConsultationPropertiesTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_givenValidSettings_expectNoViolations() {
        assertThat(validate(Duration.ofMinutes(2), Duration.ofMinutes(5), 3)).isEmpty();
    }

    @Test
    void validate_givenEqualMinimumAndMaximum_expectNoViolations() {
        assertThat(validate(Duration.ofMinutes(3), Duration.ofMinutes(3), 3)).isEmpty();
    }

    @Test
    void validate_givenMaximumBelowMinimum_expectReplyDelayRangeViolation() {
        assertThat(validate(Duration.ofMinutes(5), Duration.ofMinutes(2), 3))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("replyDelayRangeValid");
    }

    @Test
    void validate_givenNegativeMinimum_expectReplyDelayRangeViolation() {
        assertThat(validate(Duration.ofMinutes(-1), Duration.ofMinutes(2), 3))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("replyDelayRangeValid");
    }

    @Test
    void validate_givenNoAttempts_expectMaxAttemptsViolation() {
        assertThat(validate(Duration.ofMinutes(2), Duration.ofMinutes(5), 0))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("maxAttempts");
    }

    private static Set<ConstraintViolation<ConsultationProperties>> validate(
            Duration minDelay, Duration maxDelay, int maxAttempts) {
        return VALIDATOR.validate(new ConsultationProperties(
                minDelay, maxDelay, Duration.ofSeconds(15), Duration.ofMinutes(1), maxAttempts));
    }
}
