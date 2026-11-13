package nl.rubium.tegenspraak.desktop.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record FilesApp(
        String id, @NotBlank String title, @Valid List<FileEntry> files) implements DesktopApp {

    public FilesApp {
        files = files == null ? List.of() : List.copyOf(files);
        UniqueIds.require(files.stream().map(FileEntry::id).toList(), "file", id);
    }

    @Override
    public List<FileEntry> items() {
        return files;
    }
}
