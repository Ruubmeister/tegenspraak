package nl.rubium.tegenspraak.useradmin.boundary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(min = 8, max = 72) String newPassword) {}
