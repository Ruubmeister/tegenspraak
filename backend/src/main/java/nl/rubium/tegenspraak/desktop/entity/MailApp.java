package nl.rubium.tegenspraak.desktop.entity;

import java.util.List;

public record MailApp(String id, String title, List<MailMessage> messages) implements DesktopApp {

    public MailApp {
        messages = messages == null ? List.of() : List.copyOf(messages);
        UniqueIds.require(messages.stream().map(MailMessage::id).toList(), "mail", id);
    }

    @Override
    public List<MailMessage> items() {
        return messages;
    }
}
