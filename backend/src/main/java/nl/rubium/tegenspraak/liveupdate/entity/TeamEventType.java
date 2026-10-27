package nl.rubium.tegenspraak.liveupdate.entity;

/** What changed for a team, so its clients know what to load again. */
public enum TeamEventType {
    /** A conversation got new messages. */
    CHAT,
    /** Something in a case was unlocked: a contact, a desktop or an item. */
    CASE,
    /** A verdict attempt was submitted. */
    VERDICT,
    /** A player joined or left the team. */
    MEMBERS
}
