package nl.rubium.tegenspraak.desktop.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhotoTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {"../geheim.jpg", "map/foto.jpg", "foto.svg", "Foto.JPG", "foto", ""})
    void constructor_givenImageThatIsNoPlainImageFileName_expectIllegalArgument(String image) {
        assertThatThrownBy(() -> photo(image)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_givenMissingDescription_expectDescriptionViolation() {
        Photo photo = new Photo("strand", "Vakantie", "Strand", null, "strand.jpg", " ", null);

        assertThat(VALIDATOR.validate(photo))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("description");
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
