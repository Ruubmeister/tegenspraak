package nl.rubium.tegenspraak.desktop.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CalendarApp(
        String id, @NotBlank String title, @Valid List<CalendarEvent> events) implements DesktopApp {

    public CalendarApp {
        events = events == null ? List.of() : List.copyOf(events);
        UniqueIds.require(events.stream().map(CalendarEvent::id).toList(), "event", id);
    }

    @Override
    public List<CalendarEvent> items() {
        return events;
    }
}
