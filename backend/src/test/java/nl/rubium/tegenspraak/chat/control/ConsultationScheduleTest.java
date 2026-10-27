package nl.rubium.tegenspraak.chat.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ConsultationScheduleTest {

    private static final Instant NOW = Instant.parse("2026-10-27T20:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void replyDueAt_givenDelayRange_expectDueTimeWithinRange() {
        ConsultationSchedule schedule = schedule(Duration.ofMinutes(2), Duration.ofMinutes(5), 3);

        for (int sample = 0; sample < 100; sample++) {
            assertThat(schedule.replyDueAt(NOW))
                    .isBetween(NOW.plus(Duration.ofMinutes(2)), NOW.plus(Duration.ofMinutes(5)));
        }
    }

    @Test
    void replyDueAt_givenEqualMinimumAndMaximum_expectExactDelay() {
        ConsultationSchedule schedule = schedule(Duration.ofMinutes(3), Duration.ofMinutes(3), 3);

        assertThat(schedule.replyDueAt(NOW)).isEqualTo(NOW.plus(Duration.ofMinutes(3)));
    }

    @Test
    void retryDueAt_givenFailuresLeft_expectDelayGrowingWithFailures() {
        ConsultationSchedule schedule = schedule(Duration.ofMinutes(2), Duration.ofMinutes(5), 3);

        assertThat(schedule.retryDueAt(1)).contains(NOW.plus(Duration.ofMinutes(1)));
        assertThat(schedule.retryDueAt(2)).contains(NOW.plus(Duration.ofMinutes(2)));
    }

    @Test
    void retryDueAt_givenMaxAttemptsReached_expectEmpty() {
        ConsultationSchedule schedule = schedule(Duration.ofMinutes(2), Duration.ofMinutes(5), 3);

        assertThat(schedule.retryDueAt(3)).isEmpty();
    }

    @Test
    void constructor_givenMaximumBelowMinimum_expectIllegalArgument() {
        assertThatThrownBy(() -> schedule(Duration.ofMinutes(5), Duration.ofMinutes(2), 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_givenNoAttempts_expectIllegalArgument() {
        assertThatThrownBy(() -> schedule(Duration.ofMinutes(2), Duration.ofMinutes(5), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ConsultationSchedule schedule(Duration minDelay, Duration maxDelay, int maxAttempts) {
        return new ConsultationSchedule(CLOCK, minDelay, maxDelay, Duration.ofMinutes(1), maxAttempts);
    }
}
