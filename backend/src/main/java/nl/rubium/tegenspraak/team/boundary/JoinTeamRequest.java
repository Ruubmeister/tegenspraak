package nl.rubium.tegenspraak.team.boundary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinTeamRequest(@NotBlank @Size(max = 20) String joinCode) {}
