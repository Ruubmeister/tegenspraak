package nl.rubium.tegenspraak.chat.control;

import java.util.UUID;

/** Identifies the shared conversation of a team with one character of a case. */
public record Conversation(UUID teamId, String caseId, String characterId) {}
