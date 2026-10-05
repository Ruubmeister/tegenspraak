package nl.rubium.tegenspraak.casefile.entity;

import com.fasterxml.jackson.annotation.JacksonInject;
import java.util.List;


public record CharacterProfile(
        @JacksonInject("id") String id,
        String name,
        String role,
        String description,
        String personality,
        String speechStyle,
        List<String> knownFacts,
        List<CharacterSecret> secrets) {

    public CharacterProfile {
        knownFacts = knownFacts == null ? List.of() : List.copyOf(knownFacts);
        secrets = secrets == null ? List.of() : List.copyOf(secrets);
    }
}
