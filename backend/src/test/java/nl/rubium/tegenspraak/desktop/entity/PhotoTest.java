package nl.rubium.tegenspraak.desktop.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhotoTest {

    @ParameterizedTest
    @ValueSource(strings = {"../geheim.jpg", "map/foto.jpg", "foto.svg", "Foto.JPG", "foto", ""})
    void constructor_givenImageThatIsNoPlainImageFileName_expectIllegalArgument(String image) {
        assertThatThrownBy(() -> photo(image)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_givenMissingDescription_expectIllegalArgument() {
        assertThatThrownBy(() -> new Photo("strand", "Vakantie", "Strand", null, "strand.jpg", " ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strand");
    }

    @ParameterizedTest
    @CsvSource({"strand.jpg, image/jpeg", "strand.jpeg, image/jpeg", "strand.png, image/png", "strand.webp, image/webp"
    })
    void mediaType_givenExtension_expectMatchingMediaType(String image, String mediaType) {
        assertThat(photo(image).mediaType()).isEqualTo(mediaType);
    }

    private static Photo photo(String image) {
        return new Photo("strand", "Vakantie", "Strand", null, image, "Een strand.", null);
    }
}
