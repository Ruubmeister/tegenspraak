package nl.rubium.tegenspraak.casefile.entity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import nl.rubium.tegenspraak.desktop.entity.Desktop;

public record CaseDefinition(
        String id, CaseManifest manifest, List<CharacterProfile> characters, List<Desktop> desktops) {

    public CaseDefinition {
        characters = List.copyOf(characters);
        desktops = List.copyOf(desktops);
        requireValidIntroductions(characters);
        requireExistingEvidence(characters, desktops);
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

    private static boolean exists(RequiredEvidence evidence, List<Desktop> desktops) {
        return desktops.stream()
                .filter(desktop -> desktop.id().equals(evidence.desktop()))
                .flatMap(desktop -> desktop.findApp(evidence.app()).stream())
                .anyMatch(app -> app.findItem(evidence.item()).isPresent());
    }

    /**
     * Introductions must point to hidden characters of this case, and every hidden character needs at least one
     * introducer; otherwise the team could never reach it.
     */
    private static void requireValidIntroductions(List<CharacterProfile> characters) {
        Set<String> hiddenIds = characters.stream()
                .filter(CharacterProfile::hidden)
                .map(CharacterProfile::id)
                .collect(Collectors.toSet());
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
                .flatMap(character -> character.introduces().stream())
                .map(ContactIntroduction::character)
                .collect(Collectors.toSet());
        hiddenIds.stream()
                .filter(hiddenId -> !introducedIds.contains(hiddenId))
                .findFirst()
                .ifPresent(hiddenId -> {
                    throw new IllegalArgumentException(
                            "Hidden character '" + hiddenId + "' is not introduced by any other character");
                });
    }
}
