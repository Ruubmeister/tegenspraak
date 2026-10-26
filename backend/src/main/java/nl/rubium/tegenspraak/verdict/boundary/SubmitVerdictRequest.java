package nl.rubium.tegenspraak.verdict.boundary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/** The typed answer per question id. */
public record SubmitVerdictRequest(
        @NotNull @Size(max = 20) Map<@NotBlank String, @NotBlank @Size(max = MAX_ANSWER_LENGTH) String> answers) {

    public static final int MAX_ANSWER_LENGTH = 1000;
}
