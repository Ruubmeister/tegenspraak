package nl.rubium.tegenspraak.desktop.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class LockTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private final Lock lock = new Lock("Kat 2019", "Naam van de kat en het jaar");

    @Test
    void opensWith_givenSameCodeInOtherCaseAndSpacing_expectOpen() {
        assertThat(lock.opensWith("  kat2019 ")).isTrue();
    }

    @Test
    void opensWith_givenOtherCode_expectClosed() {
        assertThat(lock.opensWith("kat2018")).isFalse();
        assertThat(lock.opensWith(null)).isFalse();
    }

    @Test
    void constructor_givenBlankCode_expectIllegalArgument() {
        assertThatThrownBy(() -> new Lock("  ", "Hint")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_givenMissingHint_expectHintViolation() {
        assertThat(VALIDATOR.validate(new Lock("1234", null)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("hint");
    }

    @Test
    void filesApp_givenDuplicateFileIds_expectIllegalArgument() {
        FileEntry file = new FileEntry("menu", "Documenten", "menu.txt", "Soep", null);

        assertThatThrownBy(() -> new FilesApp("bestanden", "Bestanden", List.of(file, file)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("menu");
    }
}
