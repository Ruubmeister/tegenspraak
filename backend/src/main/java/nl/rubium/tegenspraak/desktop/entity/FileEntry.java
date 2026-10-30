package nl.rubium.tegenspraak.desktop.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** A text document in a folder of the files app. A lock is optional. */
public record FileEntry(
        String id,
        @NotBlank String folder,
        @NotBlank String name,
        @NotBlank String content,
        @Valid Lock lock) implements LockableItem {}
