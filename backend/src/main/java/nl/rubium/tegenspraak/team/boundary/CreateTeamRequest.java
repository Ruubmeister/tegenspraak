package nl.rubium.tegenspraak.team.boundary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(@NotBlank @Size(max = 50) String name) {}
