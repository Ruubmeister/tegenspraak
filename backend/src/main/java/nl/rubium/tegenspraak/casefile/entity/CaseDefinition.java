package nl.rubium.tegenspraak.casefile.entity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.ItemRef;

public record CaseDefinition(
        String id, CaseManifest manifest, List<CharacterProfile> characters, List<Desktop> desktops) {

    public CaseDefinition {
        characters = List.copyOf(characters);
        desktops = List.copyOf(desktops);
        requireValidIntroductions(characters);
        requireExistingEvidence(characters, desktops);
        requireValidConsultations(characters, desktops);
    }

    public Optional<CharacterProfile> findCharacter(String characterId) {
        return characters.stream()
                .filter(character -> character.id().equals(characterId))
                .findFirst();
    }

    public Optional<Desktop> findDesktop(String desktopId) {
        return desktops.stream()
                .filter(desktop -> desktop.id().equals(desktopId))
                .findFirst();
    }

    /** Evidence a secret requires must exist, otherwise the team could never get the character to reveal it. */
    private static void requireExistingEvidence(List<CharacterProfile> characters, List<Desktop> desktops) {
        for (CharacterProfile character : characters) {
            character.secrets().stream()
                    .flatMap(secret -> secret.requiresEvidence().stream())
                    .filter(evidence -> !exists(evidence, desktops))
                    .findFirst()
                    .ifPresent(evidence -> {
                        throw new IllegalArgumentException("A secret of character '" + character.id() + "' requires "
                                + evidence + ", which is not in this case");
                    });
        }
    }

    /**
     * A consultation must ask another character of this case, and everything it unlocks must exist; a character it
     * unlocks must be hidden, like an introduced one.
     */
    private static void requireValidConsultations(List<CharacterProfile> characters, List<Desktop> desktops) {
        Set<String> characterIds = characters.stream().map(CharacterProfile::id).collect(Collectors.toSet());
        Set<String> hiddenIds = hiddenIds(characters);
        for (CharacterProfile character : characters) {
            for (ContactConsultation consultation : character.consults()) {
                String description = "Consultation '" + consultation.id() + "' of character '" + character.id() + "'";
                if (!characterIds.contains(consultation.character())) {
                    throw new IllegalArgumentException(
                            description + " asks '" + consultation.character() + "', who is not in this case");
                }
                if (consultation.character().equals(character.id())) {
                    throw new IllegalArgumentException(description + " asks the character itself");
                }
                for (ConsultationUnlock unlock : consultation.unlocks()) {
                    if (unlock.isCharacter() && !hiddenIds.contains(unlock.character())) {
                        throw new IllegalArgumentException(description + " unlocks '" + unlock.character()
                                + "', which is not a hidden character of this case");
                    }
                    if (!unlock.isCharacter() && !exists(unlock, desktops)) {
                        throw new IllegalArgumentException(
                                description + " unlocks " + unlock + ", which is not in this case");
                    }
                }
            }
        }
    }

    private static boolean exists(ConsultationUnlock unlock, List<Desktop> desktops) {
        if (unlock.isItem()) {
            return exists(unlock.itemRef(), desktops);
        }
        return desktops.stream().anyMatch(desktop -> desktop.id().equals(unlock.desktop()));
    }

    private static boolean exists(ItemRef ref, List<Desktop> desktops) {
        return desktops.stream()
                .filter(desktop -> desktop.id().equals(ref.desktopId()))
                .flatMap(desktop -> desktop.findApp(ref.appId()).stream())
                .anyMatch(app -> app.findItem(ref.itemId()).isPresent());
    }

    /**
     * Introductions must point to hidden characters of this case, and every hidden character needs at least one
     * introducer or a consultation that unlocks it; otherwise the team could never reach it.
     */
    private static void requireValidIntroductions(List<CharacterProfile> characters) {
        Set<String> hiddenIds = hiddenIds(characters);
        for (CharacterProfile character : characters) {
            for (ContactIntroduction introduction : character.introduces()) {
                if (!hiddenIds.contains(introduction.character())) {
                    throw new IllegalArgumentException("Character '" + character.id() + "' introduces '"
                            + introduction.character() + "', which is not a hidden character of this case");
                }
                if (introduction.character().equals(character.id())) {
                    throw new IllegalArgumentException("Character '" + character.id() + "' introduces itself");
                }
            }
        }
        Set<String> introducedIds = characters.stream()
                .flatMap(character -> Stream.concat(
                        character.introduces().stream().map(ContactIntroduction::character),
                        character.consults().stream()
                                .flatMap(consultation -> consultation.unlocks().stream())
                                .filter(ConsultationUnlock::isCharacter)
                                .map(ConsultationUnlock::character)))
                .collect(Collectors.toSet());
        hiddenIds.stream()
                .filter(hiddenId -> !introducedIds.contains(hiddenId))
                .findFirst()
                .ifPresent(hiddenId -> {
                    throw new IllegalArgumentException(
                            "Hidden character '" + hiddenId + "' is not introduced by any other character");
                });
    }

    private static Set<String> hiddenIds(List<CharacterProfile> characters) {
        return characters.stream()
                .filter(CharacterProfile::hidden)
                .map(CharacterProfile::id)
                .collect(Collectors.toSet());
    }
}
