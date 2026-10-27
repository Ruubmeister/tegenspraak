package nl.rubium.tegenspraak.chat.boundary;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** The newest message of the conversation the player has seen. */
public record MarkReadRequest(@NotNull UUID messageId) {}
