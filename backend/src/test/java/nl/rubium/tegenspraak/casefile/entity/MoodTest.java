package nl.rubium.tegenspraak.casefile.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class MoodTest {

    @ParameterizedTest
    @CsvSource({
        "-100, HOSTILE",
        "-60, HOSTILE",
        "-59, SUSPICIOUS",
        "-20, SUSPICIOUS",
        "-19, NEUTRAL",
        "19, NEUTRAL",
        "20, FRIENDLY",
        "59, FRIENDLY",
        "60, TRUSTING",
        "100, TRUSTING"
    })
    void fromScore_givenScore_expectMoodOfItsRange(int score, Mood expected) {
        assertThat(Mood.fromScore(score)).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(Mood.class)
    void fromScore_givenStartingScore_expectSameMood(Mood mood) {
        assertThat(Mood.fromScore(mood.startingScore())).isEqualTo(mood);
    }

    @Test
    void isAtLeast_givenMoods_expectOrderFromHostileToTrusting() {
        assertThat(Mood.FRIENDLY.isAtLeast(Mood.FRIENDLY)).isTrue();
        assertThat(Mood.TRUSTING.isAtLeast(Mood.FRIENDLY)).isTrue();
        assertThat(Mood.NEUTRAL.isAtLeast(Mood.FRIENDLY)).isFalse();
        assertThat(List.of(Mood.values()))
                .containsExactly(Mood.HOSTILE, Mood.SUSPICIOUS, Mood.NEUTRAL, Mood.FRIENDLY, Mood.TRUSTING);
    }

    @Test
    void allows_givenSecretWithoutMinimumMood_expectEveryMoodAllowed() {
        CharacterSecret secret = new CharacterSecret("Feit", "Als ze ernaar vragen.", null, null, null);

        assertThat(secret.allows(Mood.HOSTILE)).isTrue();
    }

    @Test
    void allows_givenSecretWithMinimumMood_expectOnlyThatMoodOrBetter() {
        CharacterSecret secret = new CharacterSecret("Feit", "Als ze ernaar vragen.", null, null, Mood.FRIENDLY);

        assertThat(secret.allows(Mood.NEUTRAL)).isFalse();
        assertThat(secret.allows(Mood.TRUSTING)).isTrue();
    }

    @Test
    void mayIntroduce_givenIntroductionWithMinimumMood_expectOnlyThatMoodOrBetter() {
        CharacterProfile character = new CharacterProfile(
                "de-butler",
                "De Butler",
                "Rol",
                "Beschrijving",
                null,
                null,
                null,
                null,
                false,
                List.of(new ContactIntroduction("de-tuinman", "Als ze ernaar vragen.", Mood.TRUSTING)),
                null);

        assertThat(character.mayIntroduce("de-tuinman", Mood.FRIENDLY)).isFalse();
        assertThat(character.mayIntroduce("de-tuinman", Mood.TRUSTING)).isTrue();
        assertThat(character.mayIntroduce("de-kok", Mood.TRUSTING)).isFalse();
    }

    @Test
    void constructor_givenCharacterWithoutStartingMood_expectNeutral() {
        CharacterProfile character = new CharacterProfile(
                "de-butler", "De Butler", "Rol", "Beschrijving", null, null, null, null, false, null, null);

        assertThat(character.startingMood()).isEqualTo(Mood.NEUTRAL);
    }
}
