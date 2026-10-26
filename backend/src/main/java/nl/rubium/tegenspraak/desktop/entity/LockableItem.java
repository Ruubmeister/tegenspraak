package nl.rubium.tegenspraak.desktop.entity;

/** An item of a desktop app that can be locked with a code. Its id is unique within its app. */
public sealed interface LockableItem permits MailMessage, FileEntry, CalendarEvent, Photo, BrowserPage {

    String id();

    /** Null when the item is not locked. */
    Lock lock();
}
