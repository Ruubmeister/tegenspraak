package nl.rubium.tegenspraak.casefile.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContactConsultationTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_givenBlankAnswer_expectAnswerViolation() {
        ContactConsultation consultation =
                new ContactConsultation("vraag", "de-kok", "Onderwerp", "Als.", " ", null, null);

        assertThat(VALIDATOR.validate(consultation))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("answer");
    }

    @Test
    void allows_givenMoodBelowMinimum_expectFalse() {
        ContactConsultation consultation =
                new ContactConsultation("vraag", "de-kok", "Onderwerp", "Als.", "Antwoord.", Mood.FRIENDLY, null);

        assertThat(consultation.allows(Mood.NEUTRAL)).isFalse();
        assertThat(consultation.allows(Mood.TRUSTING)).isTrue();
    }

    @Test
    void consultationUnlock_givenCharacterDesktopOrItem_expectValid() {
        assertThatCode(() -> {
                    new ConsultationUnlock("de-tuinman", null, null, null);
                    new ConsultationUnlock(null, "landhuis", null, null);
                    new ConsultationUnlock(null, "landhuis", "bestanden", "menu");
                })
                .doesNotThrowAnyException();
    }

    @Test
    void consultationUnlock_givenCharacterAndDesktop_expectIllegalArgument() {
        assertThatThrownBy(() -> new ConsultationUnlock("de-tuinman", "landhuis", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void consultationUnlock_givenAppWithoutItem_expectIllegalArgument() {
        assertThatThrownBy(() -> new ConsultationUnlock(null, "landhuis", "bestanden", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void characterProfile_givenTwoConsultationsWithSameId_expectIllegalArgument() {
        ContactConsultation consultation =
                new ContactConsultation("vraag", "de-kok", "Onderwerp", "Als.", "Antwoord.", null, null);

        assertThatThrownBy(() -> new CharacterProfile(
                        "de-butler",
                        "De Butler",
                        "Rol",
                        "Beschrijving",
                        null,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        List.of(consultation, consultation)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same id");
    }
}
