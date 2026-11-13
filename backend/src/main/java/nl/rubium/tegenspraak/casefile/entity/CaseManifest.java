package nl.rubium.tegenspraak.casefile.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CaseManifest(
        @NotBlank String title,
        @NotBlank String summary,
        @NotNull CaseDifficulty difficulty,
        @NotBlank String introduction,
        @NotBlank String setting,
        @NotNull @Valid CaseSolution solution) {}
