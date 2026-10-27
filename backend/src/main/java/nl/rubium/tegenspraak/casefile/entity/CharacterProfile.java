package nl.rubium.tegenspraak.casefile.entity;

import com.fasterxml.jackson.annotation.JacksonInject;
import java.util.List;
import java.util.Optional;

/**
 * Persona of a character the team can chat with. A hidden character only becomes available once another character
 * introduces it, see {@link ContactIntroduction}. The starting mood is how the character feels about a team before it
 * has said anything. A character can ask other characters for information, see {@link ContactConsultation}.
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
        Mood startingMood,
        List<ContactConsultation> consults) {

    public CharacterProfile {
        knownFacts = knownFacts == null ? List.of() : List.copyOf(knownFacts);
        secrets = secrets == null ? List.of() : List.copyOf(secrets);
        introduces = introduces == null ? List.of() : List.copyOf(introduces);
        // Optional in the case files; characters are visible unless marked hidden
        hidden = Boolean.TRUE.equals(hidden);
        startingMood = startingMood == null ? Mood.NEUTRAL : startingMood;
        consults = consults == null ? List.of() : List.copyOf(consults);
        if (consults.stream().map(ContactConsultation::id).distinct().count() != consults.size()) {
            throw new IllegalArgumentException("Character '" + id + "' has two consultations with the same id");
        }
    }

    public Optional<ContactConsultation> findConsultation(String consultationId) {
        return consults.stream()
                .filter(consultation -> consultation.id().equals(consultationId))
                .findFirst();
    }

    /** True when this character introduces the contact and the introduction allows the given mood. */
    public boolean mayIntroduce(String contactId, Mood mood) {
        return introduces.stream()
                .anyMatch(introduction -> introduction.character().equals(contactId) && introduction.allows(mood));
    }
}
