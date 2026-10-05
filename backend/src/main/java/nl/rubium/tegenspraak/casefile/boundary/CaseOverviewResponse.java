package nl.rubium.tegenspraak.casefile.boundary;

import java.util.List;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.OwnerType;

/** What the player may see of a case. Deliberately excludes the setting and all persona details. */
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

    public record DesktopSummary(String id, String ownerName, OwnerType ownerType) {

        static DesktopSummary from(Desktop desktop) {
            return new DesktopSummary(desktop.id(), desktop.ownerName(), desktop.ownerType());
        }
    }

    static CaseOverviewResponse from(CaseDefinition caseDefinition) {
        return new CaseOverviewResponse(
                caseDefinition.id(),
                caseDefinition.manifest().title(),
                caseDefinition.manifest().introduction(),
                caseDefinition.characters().stream().map(CharacterSummary::from).toList(),
                caseDefinition.desktops().stream().map(DesktopSummary::from).toList());
    }
}
