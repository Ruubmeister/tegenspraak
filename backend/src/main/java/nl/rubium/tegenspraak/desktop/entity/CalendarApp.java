package nl.rubium.tegenspraak.desktop.entity;

import java.util.List;

public record CalendarApp(String id, String title, List<CalendarEvent> events) implements DesktopApp {

    public CalendarApp {
        events = events == null ? List.of() : List.copyOf(events);
        UniqueIds.require(events.stream().map(CalendarEvent::id).toList(), "event", id);
    }

    @Override
    public List<CalendarEvent> items() {
        return events;
    }
}
