package nl.rubium.tegenspraak.useradmin.boundary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** BCrypt only uses the first 72 bytes of a password, so longer passwords are rejected instead of truncated. */
public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[A-Za-z0-9_.-]+")
        String username,

        @NotBlank @Size(min = 8, max = 72) String password) {}
