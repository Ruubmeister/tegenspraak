package nl.rubium.tegenspraak.casefile.entity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaseDefinitionTest {

    @Test
    void constructor_givenHiddenCharacterIntroducedByOther_expectValid() {
        assertThatCode(() -> caseWith(character("de-butler", false, "de-tuinman"), character("de-tuinman", true)))
                .doesNotThrowAnyException();
    }

    @Test
    void constructor_givenIntroductionOfVisibleCharacter_expectIllegalArgument() {
        assertThatThrownBy(() -> caseWith(character("de-butler", false, "de-tuinman"), character("de-tuinman", false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a hidden character");
    }

    @Test
    void constructor_givenIntroductionOfUnknownCharacter_expectIllegalArgument() {
        assertThatThrownBy(() -> caseWith(character("de-butler", false, "de-kok")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("de-kok");
    }

    @Test
    void constructor_givenHiddenCharacterThatIntroducesItself_expectIllegalArgument() {
        assertThatThrownBy(() -> caseWith(character("de-tuinman", true, "de-tuinman")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introduces itself");
    }

    @Test
    void constructor_givenHiddenCharacterWithoutIntroducer_expectIllegalArgument() {
        assertThatThrownBy(() -> caseWith(character("de-butler", false), character("de-tuinman", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not introduced");
    }

    private static CaseDefinition caseWith(CharacterProfile... characters) {
        return new CaseDefinition("demo", CaseFixtures.manifestWithSetting("Geheim"), List.of(characters), List.of());
    }

    private static CharacterProfile character(String id, boolean hidden, String... introduces) {
        List<ContactIntroduction> introductions = Arrays.stream(introduces)
                .map(introduced -> new ContactIntroduction(introduced, "Als de speler ernaar vraagt."))
                .toList();
        return new CharacterProfile(id, id, "Rol", "Beschrijving", null, null, null, null, hidden, introductions);
    }
}
