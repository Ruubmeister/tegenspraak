package nl.rubium.tegenspraak.casefile.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import nl.rubium.tegenspraak.desktop.entity.ItemRef;
import org.junit.jupiter.api.Test;

class CharacterSecretTest {

    private static final ItemRef LETTER = new ItemRef("landhuis", "mail", "brief");
    private static final ItemRef WILL = new ItemRef("landhuis", "bestanden", "testament");

    @Test
    void isEvidenceShown_givenNoRequiredEvidence_expectTrue() {
        CharacterSecret secret = new CharacterSecret("Feit", "Als ze ernaar vragen.", null, null, null);

        assertThat(secret.isEvidenceShown(Set.of())).isTrue();
    }

    @Test
    void isEvidenceShown_givenOnlyPartOfRequiredEvidenceWithoutCount_expectFalse() {
        CharacterSecret secret = new CharacterSecret("Feit", "Als ze het tonen.", List.of(LETTER, WILL), null, null);

        assertThat(secret.isEvidenceShown(Set.of(LETTER))).isFalse();
    }

    @Test
    void isEvidenceShown_givenAllRequiredEvidence_expectTrue() {
        CharacterSecret secret = new CharacterSecret("Feit", "Als ze het tonen.", List.of(LETTER, WILL), null, null);

        assertThat(secret.isEvidenceShown(Set.of(LETTER, WILL))).isTrue();
    }

    @Test
    void isEvidenceShown_givenRequiredCountReached_expectTrue() {
        CharacterSecret secret = new CharacterSecret("Feit", "Als ze het tonen.", List.of(LETTER, WILL), 1, null);

        assertThat(secret.isEvidenceShown(Set.of(WILL))).isTrue();
    }

    @Test
    void isEvidenceShown_givenOnlyUnrelatedEvidence_expectFalse() {
        CharacterSecret secret = new CharacterSecret("Feit", "Als ze het tonen.", List.of(LETTER), null, null);

        assertThat(secret.isEvidenceShown(Set.of(WILL))).isFalse();
    }

    @Test
    void constructor_givenRequiredCountAboveEvidenceSize_expectIllegalArgument() {
        assertThatThrownBy(() -> new CharacterSecret("Feit", "Als ze het tonen.", List.of(LETTER), 2, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required count");
    }

    @Test
    void constructor_givenRequiredCountWithoutEvidence_expectIllegalArgument() {
        assertThatThrownBy(() -> new CharacterSecret("Feit", "Als ze het tonen.", null, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_givenSameEvidenceTwice_expectIllegalArgument() {
        assertThatThrownBy(() -> new CharacterSecret("Feit", "Als ze het tonen.", List.of(LETTER, LETTER), 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("twice");
    }
}
