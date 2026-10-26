package nl.rubium.tegenspraak.desktop.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CalendarEventTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 3, 12, 20, 0);

    @Test
    void constructor_givenEndBeforeStart_expectIllegalArgument() {
        assertThatThrownBy(() ->
                        new CalendarEvent("etentje", "Etentje", START, START.minusHours(1), null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("etentje");
    }

    @Test
    void constructor_givenMissingAttendees_expectEmptyList() {
        CalendarEvent event = new CalendarEvent("etentje", "Etentje", START, START, null, null, null, null);

        assertThat(event.attendees()).isEmpty();
    }
}
