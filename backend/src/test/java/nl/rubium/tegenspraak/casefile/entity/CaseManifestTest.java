package nl.rubium.tegenspraak.casefile.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaseManifestTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private static final CaseSolution SOLUTION =
            new CaseSolution(List.of(new SolutionQuestion("dader", "Wie deed het?", "De butler.")), "Einde.");

    @Test
    void validate_givenNoDifficulty_expectDifficultyViolation() {
        CaseManifest manifest = new CaseManifest("Demo", "Kort", null, "Intro", "Geheim", SOLUTION);

        assertThat(VALIDATOR.validate(manifest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("difficulty");
    }

    @Test
    void validate_givenQuestionWithoutAnswer_expectNestedAnswerViolation() {
        CaseSolution solution =
                new CaseSolution(List.of(new SolutionQuestion("dader", "Wie deed het?", " ")), "Einde.");
        CaseManifest manifest = new CaseManifest("Demo", "Kort", CaseDifficulty.MAKKELIJK, "Intro", "Geheim", solution);

        assertThat(VALIDATOR.validate(manifest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("solution.questions[0].answer");
    }
}
