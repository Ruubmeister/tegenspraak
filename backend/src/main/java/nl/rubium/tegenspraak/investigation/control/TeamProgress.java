package nl.rubium.tegenspraak.investigation.control;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.DesktopApp;
import nl.rubium.tegenspraak.desktop.entity.LockableItem;

/** What a team has unlocked in a case, and therefore what it may see. */
public record TeamProgress(CaseDefinition caseDefinition, Set<String> unlockedTargets) {

    public TeamProgress {
        unlockedTargets = Set.copyOf(unlockedTargets);
    }

    public List<CharacterProfile> availableCharacters() {
        return caseDefinition.characters().stream().filter(this::isAvailable).toList();
    }

    public boolean isAvailable(CharacterProfile character) {
        return !character.hidden() || isUnlocked(UnlockTarget.character(character.id()));
    }

    /** Hidden characters this character may still introduce, so its LLM gets the introduction tool for them. */
    public List<CharacterProfile> introducibleBy(CharacterProfile introducer) {
        return introducer.introduces().stream()
                .map(introduction -> caseDefinition.findCharacter(introduction.character()))
                .flatMap(Optional::stream)
                .filter(character -> !isAvailable(character))
                .toList();
    }

    public boolean isOpen(Desktop desktop) {
        return desktop.lock() == null || isUnlocked(UnlockTarget.desktop(desktop.id()));
    }

    public boolean isOpen(Desktop desktop, DesktopApp app, LockableItem item) {
        return item.lock() == null || isUnlocked(UnlockTarget.item(desktop.id(), app.id(), item.id()));
    }

    private boolean isUnlocked(UnlockTarget target) {
        return unlockedTargets.contains(target.key());
    }
}
