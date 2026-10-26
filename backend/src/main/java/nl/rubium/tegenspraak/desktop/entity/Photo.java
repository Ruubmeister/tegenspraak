package nl.rubium.tegenspraak.desktop.entity;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A picture in an album of the photos app. The image is the name of a file in the folder next to the desktop file,
 * {@code desktops/{desktop-id}/{image}}. Characters cannot see images, so the description tells them what is on it. A
 * lock is optional and hides the image and description.
 */
public record Photo(
        String id, String album, String title, LocalDateTime takenAt, String image, String description, Lock lock)
        implements LockableItem {

    /** A plain file name keeps image paths inside the desktop folder. SVG is left out, as it can carry scripts. */
    private static final Pattern IMAGE_NAME = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*\\.(jpg|jpeg|png|webp)");

    public Photo {
        if (album == null || album.isBlank() || title == null || title.isBlank()) {
            throw new IllegalArgumentException("Photo '" + id + "' needs an album and a title");
        }
        if (image == null || !IMAGE_NAME.matcher(image).matches()) {
            throw new IllegalArgumentException("Photo '" + id + "' needs an image file name such as 'strand-2025.jpg'"
                    + " in lowercase kebab-case with extension jpg, jpeg, png or webp");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Photo '" + id + "' needs a description for the characters");
        }
    }

    /** The media type, from the extension of the image. */
    public String mediaType() {
        String extension = image.substring(image.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}
