package nl.rubium.tegenspraak.casefile.entity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.FileEntry;
import nl.rubium.tegenspraak.desktop.entity.FilesApp;
import nl.rubium.tegenspraak.desktop.entity.OwnerType;
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

    @Test
    void constructor_givenSecretRequiringExistingFile_expectValid() {
        CharacterProfile butler = characterWithSecretRequiring(new RequiredEvidence("landhuis", "bestanden", "menu"));

        assertThatCode(() -> caseWithDesktop(butler)).doesNotThrowAnyException();
    }

    @Test
    void constructor_givenSecretRequiringUnknownItem_expectIllegalArgument() {
        CharacterProfile butler =
                characterWithSecretRequiring(new RequiredEvidence("landhuis", "bestanden", "testament"));

        assertThatThrownBy(() -> caseWithDesktop(butler))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("de-butler")
                .hasMessageContaining("testament");
    }

    @Test
    void constructor_givenSecretRequiringUnknownDesktop_expectIllegalArgument() {
        CharacterProfile butler = characterWithSecretRequiring(new RequiredEvidence("kasteel", "bestanden", "menu"));

        assertThatThrownBy(() -> caseWithDesktop(butler))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kasteel");
    }

    @Test
    void constructor_givenConsultationOfOtherCharacterWithExistingUnlocks_expectValid() {
        CharacterProfile butler = characterConsulting(
                "de-kok",
                new ConsultationUnlock(null, "landhuis", "bestanden", "menu"),
                new ConsultationUnlock(null, "landhuis", null, null));

        assertThatCode(() -> caseWithDesktop(butler, character("de-kok", false)))
                .doesNotThrowAnyException();
    }

    @Test
    void constructor_givenConsultationOfUnknownCharacter_expectIllegalArgument() {
        assertThatThrownBy(() -> caseWithDesktop(characterConsulting("de-kok")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("de-kok")
                .hasMessageContaining("not in this case");
    }

    @Test
    void constructor_givenConsultationOfItself_expectIllegalArgument() {
        assertThatThrownBy(() -> caseWithDesktop(characterConsulting("de-butler")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asks the character itself");
    }

    @Test
    void constructor_givenConsultationUnlockingUnknownItem_expectIllegalArgument() {
        CharacterProfile butler =
                characterConsulting("de-kok", new ConsultationUnlock(null, "landhuis", "bestanden", "testament"));

        assertThatThrownBy(() -> caseWithDesktop(butler, character("de-kok", false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("testament");
    }

    @Test
    void constructor_givenConsultationUnlockingVisibleCharacter_expectIllegalArgument() {
        CharacterProfile butler = characterConsulting("de-kok", new ConsultationUnlock("de-kok", null, null, null));

        assertThatThrownBy(() -> caseWithDesktop(butler, character("de-kok", false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a hidden character");
    }

    @Test
    void constructor_givenHiddenCharacterOnlyUnlockedByConsultation_expectValid() {
        CharacterProfile butler = characterConsulting("de-kok", new ConsultationUnlock("de-tuinman", null, null, null));

        assertThatCode(() -> caseWithDesktop(butler, character("de-kok", false), character("de-tuinman", true)))
                .doesNotThrowAnyException();
    }

    private static CharacterProfile characterConsulting(String contactId, ConsultationUnlock... unlocks) {
        ContactConsultation consultation = new ContactConsultation(
                "vraag", contactId, "Onderwerp", "Als ze ernaar vragen.", "Het antwoord.", null, List.of(unlocks));
        return new CharacterProfile(
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
                List.of(consultation));
    }

    private static CaseDefinition caseWithDesktop(CharacterProfile... characters) {
        Desktop hall = new Desktop(
                "landhuis",
                "Landhuis",
                OwnerType.ORGANISATION,
                "Hal",
                null,
                List.of(new FilesApp(
                        "bestanden",
                        "Bestanden",
                        List.of(new FileEntry("menu", "Documenten", "menu.txt", "Soep", null)))));
        return new CaseDefinition(
                "demo", CaseFixtures.manifestWithSetting("Geheim"), List.of(characters), List.of(hall));
    }

    private static CharacterProfile characterWithSecretRequiring(RequiredEvidence evidence) {
        CharacterSecret secret = new CharacterSecret("Feit", "Als ze het tonen.", List.of(evidence), null, null);
        return new CharacterProfile(
                "de-butler",
                "De Butler",
                "Rol",
                "Beschrijving",
                null,
                null,
                null,
                List.of(secret),
                false,
                null,
                null,
                null);
    }

    private static CaseDefinition caseWith(CharacterProfile... characters) {
        return new CaseDefinition("demo", CaseFixtures.manifestWithSetting("Geheim"), List.of(characters), List.of());
    }

    private static CharacterProfile character(String id, boolean hidden, String... introduces) {
        List<ContactIntroduction> introductions = Arrays.stream(introduces)
                .map(introduced -> new ContactIntroduction(introduced, "Als de speler ernaar vraagt.", null))
                .toList();
        return new CharacterProfile(
                id, id, "Rol", "Beschrijving", null, null, null, null, hidden, introductions, null, null);
    }
}
