package nl.rubium.tegenspraak.casefile.entity;

import com.fasterxml.jackson.annotation.JacksonInject;
import java.util.List;

/**
 * Persona of a character the team can chat with. A hidden character only becomes available once another character
 * introduces it, see {@link ContactIntroduction}. The starting mood is how the character feels about a team before it
 * has said anything.
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
        List<ContactIntroduction> introduces,
        Mood startingMood) {

    public CharacterProfile {
        knownFacts = knownFacts == null ? List.of() : List.copyOf(knownFacts);
        secrets = secrets == null ? List.of() : List.copyOf(secrets);
        introduces = introduces == null ? List.of() : List.copyOf(introduces);
        // Optional in the case files; characters are visible unless marked hidden
        hidden = Boolean.TRUE.equals(hidden);
        startingMood = startingMood == null ? Mood.NEUTRAL : startingMood;
    }

    /** True when this character introduces the contact and the introduction allows the given mood. */
    public boolean mayIntroduce(String contactId, Mood mood) {
        return introduces.stream()
                .anyMatch(introduction -> introduction.character().equals(contactId) && introduction.allows(mood));
    }
}
