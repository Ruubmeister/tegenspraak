package nl.rubium.tegenspraak.chat.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import nl.rubium.tegenspraak.casefile.entity.Mood;
import org.junit.jupiter.api.Test;

class CharacterMoodTest {

    private static final UUID TEAM_ID = UUID.randomUUID();

    @Test
    void start_givenStartingMood_expectStartingScoreOfThatMood() {
        CharacterMood mood = CharacterMood.start(TEAM_ID, "demo", "de-butler", Mood.SUSPICIOUS);

        assertThat(mood.getScore()).isEqualTo(Mood.SUSPICIOUS.startingScore());
        assertThat(mood.getMood()).isEqualTo(Mood.SUSPICIOUS);
    }

    @Test
    void changeScore_givenChangesAcrossThreshold_expectMoodFollowsScore() {
        CharacterMood mood = CharacterMood.start(TEAM_ID, "demo", "de-butler", Mood.NEUTRAL);

        mood.changeScore(15);
        mood.changeScore(15);

        assertThat(mood.getScore()).isEqualTo(30);
        assertThat(mood.getMood()).isEqualTo(Mood.FRIENDLY);
    }

    @Test
    void changeScore_givenChangeBeyondMaximum_expectScoreClampedToMaximum() {
        CharacterMood mood = CharacterMood.start(TEAM_ID, "demo", "de-butler", Mood.TRUSTING);

        mood.changeScore(Integer.MAX_VALUE);

        assertThat(mood.getScore()).isEqualTo(Mood.MAX_SCORE);
    }

    @Test
    void changeScore_givenChangeBeyondMinimum_expectScoreClampedToMinimum() {
        CharacterMood mood = CharacterMood.start(TEAM_ID, "demo", "de-butler", Mood.HOSTILE);

        mood.changeScore(-50);

        assertThat(mood.getScore()).isEqualTo(Mood.MIN_SCORE);
    }
}
