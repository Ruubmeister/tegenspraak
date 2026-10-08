package nl.rubium.tegenspraak.investigation.boundary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnlockRequest(@NotBlank @Size(max = 100) String code) {}
