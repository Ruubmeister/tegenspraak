package nl.rubium.tegenspraak.desktop.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record PhotosApp(
        String id, @NotBlank String title, @Valid List<Photo> photos) implements DesktopApp {

    public PhotosApp {
        photos = photos == null ? List.of() : List.copyOf(photos);
        UniqueIds.require(photos.stream().map(Photo::id).toList(), "photo", id);
    }

    @Override
    public List<Photo> items() {
        return photos;
    }
}
