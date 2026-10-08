package nl.rubium.tegenspraak.verdict.boundary;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** The chosen option id per question id. */
public record SubmitVerdictRequest(@NotNull Map<String, String> answers) {}
