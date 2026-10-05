package nl.rubium.tegenspraak.chat.control;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseManifest;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.CharacterSecret;
import org.junit.jupiter.api.Test;

class CharacterPromptBuilderTest {

    private final CharacterPromptBuilder builder = new CharacterPromptBuilder();

    @Test
    void buildSystemPrompt_givenCharacterWithSecrets_expectSecretsWithRevealConditions() {
        CharacterProfile character = character(
                List.of(new CharacterSecret("Hij was in de bibliotheek.", "De speler noemt de bibliotheek.")));

        String prompt = builder.buildSystemPrompt(caseWith(character), character);

        assertThat(prompt)
                .contains("## Geheimen")
                .contains("Geheim: Hij was in de bibliotheek.")
                .contains("Geef dit pas prijs als: De speler noemt de bibliotheek.");
    }

    @Test
    void buildSystemPrompt_givenCase_expectSettingPersonaAndRulesIncluded() {
        CharacterProfile character = character(List.of());

        String prompt = builder.buildSystemPrompt(caseWith(character), character);

        assertThat(prompt)
                .contains("Je speelt het personage De Butler in het detectivespel \"Demo\"")
                .contains("De butler deed het.")
                .contains("- Het diner begon om acht uur.")
                .contains("## Spelregels");
    }

    @Test
    void buildSystemPrompt_givenCharacterWithoutSecretsOrStyle_expectSectionsOmitted() {
        CharacterProfile character = character(List.of());

        String prompt = builder.buildSystemPrompt(caseWith(character), character);

        assertThat(prompt).doesNotContain("## Geheimen").doesNotContain("## Schrijfstijl");
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
                secrets);
    }

    private static CaseDefinition caseWith(CharacterProfile character) {
        return new CaseDefinition(
                "demo",
                new CaseManifest("Demo", "Kort", "Intro", "De butler deed het."),
                List.of(character),
                List.of());
    }
}
