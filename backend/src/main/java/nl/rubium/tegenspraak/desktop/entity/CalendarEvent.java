package nl.rubium.tegenspraak.desktop.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * An appointment in the calendar app. Location, attendees and notes are optional. A lock is optional and hides the
 * location, attendees and notes, but not the title and time.
 */
public record CalendarEvent(
        String id,
        String title,
        LocalDateTime start,
        LocalDateTime end,
        String location,
        List<String> attendees,
        String notes,
        Lock lock)
        implements LockableItem {

    public CalendarEvent {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Event '" + id + "' needs a start and an end that is not before it");
        }
        attendees = attendees == null ? List.of() : List.copyOf(attendees);
    }
}
