package nl.rubium.tegenspraak.desktop.entity;

/** A mail or file that can be locked with a code. Its id is unique within its app. */
public sealed interface LockableItem permits MailMessage, FileEntry {

    String id();

    /** Null when the item is not locked. */
    Lock lock();
}
