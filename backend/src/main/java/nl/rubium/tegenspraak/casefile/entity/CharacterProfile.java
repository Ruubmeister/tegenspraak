package nl.rubium.tegenspraak.casefile.entity;

import com.fasterxml.jackson.annotation.JacksonInject;
import java.util.List;

/**
 * Persona of a character the team can chat with. A hidden character only becomes available once another character
 * introduces it, see {@link ContactIntroduction}.
 */
public record CharacterProfile(
        @JacksonInject("id") String id,
        String name,
        String role,
        String description,
        String personality,
        String speechStyle,
        List<String> knownFacts,
        List<CharacterSecret> secrets,
        Boolean hidden,
        List<ContactIntroduction> introduces) {

    public CharacterProfile {
        knownFacts = knownFacts == null ? List.of() : List.copyOf(knownFacts);
        secrets = secrets == null ? List.of() : List.copyOf(secrets);
        introduces = introduces == null ? List.of() : List.copyOf(introduces);
        // Optional in the case files; characters are visible unless marked hidden
        hidden = Boolean.TRUE.equals(hidden);
    }
}
