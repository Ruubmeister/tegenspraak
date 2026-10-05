package nl.rubium.tegenspraak.desktop.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum OwnerType {
    @JsonProperty("person")
    PERSON,
    @JsonProperty("organisation")
    ORGANISATION
}
