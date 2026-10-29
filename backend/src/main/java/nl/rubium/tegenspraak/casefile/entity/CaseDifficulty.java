package nl.rubium.tegenspraak.casefile.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

/** How hard a case is, from easiest to hardest, so players can pick a case that suits them. */
public enum CaseDifficulty {
    @JsonProperty("oefenzaak")
    OEFENZAAK,
    @JsonProperty("makkelijk")
    MAKKELIJK,
    @JsonProperty("gemiddeld")
    GEMIDDELD,
    @JsonProperty("moeilijk")
    MOEILIJK,
    @JsonProperty("expert")
    EXPERT
}
