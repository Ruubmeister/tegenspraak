package nl.rubium.tegenspraak.casefile.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class CaseManifestTest {

    private static final CaseSolution SOLUTION =
            new CaseSolution(List.of(new SolutionQuestion("dader", "Wie deed het?", "De butler.")), "Einde.");

    @Test
    void caseManifest_givenNoDifficulty_expectRejected() {
        assertThatThrownBy(() -> new CaseManifest("Demo", "Kort", null, "Intro", "Geheim", SOLUTION))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("difficulty");
    }
}
