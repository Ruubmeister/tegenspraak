package nl.rubium.tegenspraak.investigation.control;

/** Points to an item on a desktop of a case, for example one a player attached to a chat message. */
public record EvidenceRef(String desktopId, String appId, String itemId) {}
