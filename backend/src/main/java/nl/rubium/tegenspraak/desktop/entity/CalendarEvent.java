package nl.rubium.tegenspraak.desktop.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * An appointment in the calendar app. Location, attendees and notes are optional. A lock is optional and hides the
 * location, attendees and notes, but not the title and time.
 */
public record CalendarEvent(
        String id,
        @NotBlank String title,
        @NotNull LocalDateTime start,
        @NotNull LocalDateTime end,
        String location,
        List<String> attendees,
        String notes,
        @Valid Lock lock)
        implements LockableItem {

    public CalendarEvent {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("Event '" + id + "' ends before it starts");
        }
        attendees = attendees == null ? List.of() : List.copyOf(attendees);
    }
}
