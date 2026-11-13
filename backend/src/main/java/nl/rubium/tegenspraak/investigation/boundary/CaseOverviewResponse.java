package nl.rubium.tegenspraak.investigation.boundary;

import java.util.List;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.OwnerType;
import nl.rubium.tegenspraak.investigation.control.TeamProgress;
import org.jspecify.annotations.Nullable;

/**
 * What a team may see of a case. Excludes the setting, all persona details and characters that were not introduced
 * yet. Locked desktops are listed with their hint.
 */
public record CaseOverviewResponse(
        String id,
        String title,
        String introduction,
        List<CharacterSummary> characters,
        List<DesktopSummary> desktops) {

    public record CharacterSummary(String id, String name, String role, String description) {

        static CharacterSummary from(CharacterProfile character) {
            return new CharacterSummary(character.id(), character.name(), character.role(), character.description());
        }
    }

    public record DesktopSummary(
            String id,
            String ownerName,
            OwnerType ownerType,
            @Nullable LockResponse lock) {

        static DesktopSummary from(Desktop desktop, TeamProgress progress) {
            return new DesktopSummary(
                    desktop.id(),
                    desktop.ownerName(),
                    desktop.ownerType(),
                    LockResponse.whileLocked(desktop.lock(), progress.isOpen(desktop)));
        }
    }

    static CaseOverviewResponse from(TeamProgress progress) {
        CaseDefinition caseDefinition = progress.caseDefinition();
        return new CaseOverviewResponse(
                caseDefinition.id(),
                caseDefinition.manifest().title(),
                caseDefinition.manifest().introduction(),
                progress.availableCharacters().stream()
                        .map(CharacterSummary::from)
                        .toList(),
                caseDefinition.desktops().stream()
                        .map(desktop -> DesktopSummary.from(desktop, progress))
                        .toList());
    }
}
