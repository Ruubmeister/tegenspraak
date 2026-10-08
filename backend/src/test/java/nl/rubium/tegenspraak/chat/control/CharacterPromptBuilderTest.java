package nl.rubium.tegenspraak.chat.control;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseFixtures;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.CharacterSecret;
import nl.rubium.tegenspraak.casefile.entity.ContactIntroduction;
import org.junit.jupiter.api.Test;

class CharacterPromptBuilderTest {

    private final CharacterPromptBuilder builder = new CharacterPromptBuilder();

    @Test
    void buildSystemPrompt_givenCharacterWithSecrets_expectSecretsWithRevealConditions() {
        CharacterProfile character = character(
                List.of(new CharacterSecret("Hij was in de bibliotheek.", "De speler noemt de bibliotheek.")));

        String prompt = builder.buildSystemPrompt(caseWith(character), character, List.of());

        assertThat(prompt)
                .contains("## Geheimen")
                .contains("Geheim: Hij was in de bibliotheek.")
                .contains("Geef dit pas prijs als: De speler noemt de bibliotheek.");
    }

    @Test
    void buildSystemPrompt_givenCase_expectSettingPersonaAndRulesIncluded() {
        CharacterProfile character = character(List.of());

        String prompt = builder.buildSystemPrompt(caseWith(character), character, List.of());

        assertThat(prompt)
                .contains("Je speelt het personage De Butler in het detectivespel \"Demo\"")
                .contains("De butler deed het.")
                .contains("- Het diner begon om acht uur.")
                .contains("## Spelregels");
    }

    @Test
    void buildSystemPrompt_givenCase_expectTeamChatWithNamedMessagesExplained() {
        CharacterProfile character = character(List.of());

        String prompt = builder.buildSystemPrompt(caseWith(character), character, List.of());

        assertThat(prompt)
                .contains("Je chat met een team van rechercheurs")
                .contains("naam van de rechercheur tussen blokhaken");
    }

    @Test
    void buildSystemPrompt_givenCharacterWithoutSecretsOrStyle_expectSectionsOmitted() {
        CharacterProfile character = character(List.of());

        String prompt = builder.buildSystemPrompt(caseWith(character), character, List.of());

        assertThat(prompt)
                .doesNotContain("## Geheimen")
                .doesNotContain("## Schrijfstijl")
                .doesNotContain("## Mensen die je kunt voorstellen");
    }

    @Test
    void buildSystemPrompt_givenIntroducibleContact_expectContactWithConditionAndToolName() {
        CharacterProfile gardener = new CharacterProfile(
                "de-tuinman", "De Tuinman", "Tuinman", "In de kas.", null, null, null, null, true, null);
        CharacterProfile butler = new CharacterProfile(
                "de-butler",
                "De Butler",
                "Butler",
                "Altijd aanwezig.",
                null,
                null,
                null,
                null,
                false,
                List.of(new ContactIntroduction("de-tuinman", "De speler vraagt naar de tuin.")));
        CaseDefinition caseDefinition = new CaseDefinition(
                "demo", CaseFixtures.manifestWithSetting("Geheim"), List.of(butler, gardener), List.of());

        String prompt = builder.buildSystemPrompt(caseDefinition, butler, List.of(gardener));

        assertThat(prompt)
                .contains("## Mensen die je kunt voorstellen")
                .contains("- De Tuinman (id: de-tuinman)")
                .contains("Voorwaarde: De speler vraagt naar de tuin.")
                .contains("introduce_contact");
    }

    private static CharacterProfile character(List<CharacterSecret> secrets) {
        return new CharacterProfile(
                "de-butler",
                "De Butler",
                "Butler",
                "Altijd aanwezig.",
                "Stijf",
                null,
                List.of("Het diner begon om acht uur."),
                secrets,
                false,
                List.of());
    }

    private static CaseDefinition caseWith(CharacterProfile character) {
        return new CaseDefinition(
                "demo", CaseFixtures.manifestWithSetting("De butler deed het."), List.of(character), List.of());
    }
}
