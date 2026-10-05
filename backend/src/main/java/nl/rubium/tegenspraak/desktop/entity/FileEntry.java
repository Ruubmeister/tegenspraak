package nl.rubium.tegenspraak.desktop.entity;

/** A text document in a folder of the files app. */
public record FileEntry(String id, String folder, String name, String content) {}
