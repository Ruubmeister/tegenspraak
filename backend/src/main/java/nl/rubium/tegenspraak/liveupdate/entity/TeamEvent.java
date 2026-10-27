package nl.rubium.tegenspraak.liveupdate.entity;

import java.util.UUID;

/**
 * Something changed for a team. Services publish it as a Spring application event; it is pushed to the team's open
 * clients once the change is committed. It only says where to look, never what changed, so the clients load the data
 * through the regular endpoints with their own access checks. Ids that do not apply are null.
 */
public record TeamEvent(UUID teamId, TeamEventType type, String caseId, String characterId) {

    public static TeamEvent chat(UUID teamId, String caseId, String characterId) {
        return new TeamEvent(teamId, TeamEventType.CHAT, caseId, characterId);
    }

    public static TeamEvent caseProgress(UUID teamId, String caseId) {
        return new TeamEvent(teamId, TeamEventType.CASE, caseId, null);
    }

    public static TeamEvent verdict(UUID teamId, String caseId) {
        return new TeamEvent(teamId, TeamEventType.VERDICT, caseId, null);
    }

    public static TeamEvent members(UUID teamId) {
        return new TeamEvent(teamId, TeamEventType.MEMBERS, null, null);
    }
}
