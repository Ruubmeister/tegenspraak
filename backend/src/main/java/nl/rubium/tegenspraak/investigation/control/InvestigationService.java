package nl.rubium.tegenspraak.investigation.control;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.casefile.control.CaseCatalog;
import nl.rubium.tegenspraak.casefile.control.CaseContentNotFoundException;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.DesktopApp;
import nl.rubium.tegenspraak.desktop.entity.Lock;
import nl.rubium.tegenspraak.desktop.entity.LockableItem;
import nl.rubium.tegenspraak.desktop.entity.Photo;
import nl.rubium.tegenspraak.investigation.entity.TeamUnlockRepository;
import nl.rubium.tegenspraak.investigation.entity.WrongCodeAttempt;
import nl.rubium.tegenspraak.investigation.entity.WrongCodeAttemptRepository;
import nl.rubium.tegenspraak.team.control.TeamService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides what a team can see and use in a case: hidden characters appear once introduced, locked desktops, mails and
 * files open with their code. Unlocks are shared by the whole team. Wrong codes are limited per target within a
 * sliding window to slow down guessing.
 */
@Service
public class InvestigationService {

    private final CaseCatalog caseCatalog;
    private final TeamService teamService;
    private final TeamUnlockRepository teamUnlockRepository;
    private final WrongCodeAttemptRepository wrongCodeAttemptRepository;
    private final Clock clock;
    private final int maxWrongAttempts;
    private final Duration wrongAttemptWindow;

    public InvestigationService(
            CaseCatalog caseCatalog,
            TeamService teamService,
            TeamUnlockRepository teamUnlockRepository,
            WrongCodeAttemptRepository wrongCodeAttemptRepository,
            Clock clock,
            @Value("${tegenspraak.locks.max-wrong-attempts}") int maxWrongAttempts,
            @Value("${tegenspraak.locks.wrong-attempt-window}") Duration wrongAttemptWindow) {
        this.caseCatalog = caseCatalog;
        this.teamService = teamService;
        this.teamUnlockRepository = teamUnlockRepository;
        this.wrongCodeAttemptRepository = wrongCodeAttemptRepository;
        this.clock = clock;
        this.maxWrongAttempts = maxWrongAttempts;
        this.wrongAttemptWindow = wrongAttemptWindow;
    }

    public TeamProgress getProgress(UUID teamId, UUID playerId, String caseId) {
        teamService.requireMember(teamId, playerId);
        CaseDefinition caseDefinition = caseCatalog.getCase(caseId);
        return new TeamProgress(caseDefinition, new HashSet<>(teamUnlockRepository.findTargets(teamId, caseId)));
    }

    /** Hidden characters that were not introduced yet do not exist for the team. */
    public CharacterProfile requireAvailableCharacter(UUID teamId, UUID playerId, String caseId, String characterId) {
        TeamProgress progress = getProgress(teamId, playerId, caseId);
        CharacterProfile character = caseCatalog.getCharacter(caseId, characterId);
        if (!progress.isAvailable(character)) {
            throw new CaseContentNotFoundException("Character not available yet: " + characterId);
        }
        return character;
    }

    /**
     * Makes introduced characters available to the team.
     *
     * @return the characters that were newly introduced, in the given order
     */
    @Transactional
    public List<CharacterProfile> introduceCharacters(UUID teamId, String caseId, List<CharacterProfile> characters) {
        return characters.stream()
                .filter(character -> unlock(teamId, caseId, UnlockTarget.character(character.id())))
                .toList();
    }

    /** A photo whose image the team may see: it exists, and neither its desktop nor the photo itself is locked. */
    public Photo requireOpenPhoto(UUID teamId, UUID playerId, String caseId, EvidenceRef ref) {
        return getProgress(teamId, playerId, caseId)
                .findOpenEvidence(ref)
                .map(EvidenceItem::item)
                .filter(Photo.class::isInstance)
                .map(Photo.class::cast)
                .orElseThrow(() -> new CaseContentNotFoundException("No open photo: " + ref));
    }

    /** A wrong code must not roll back, or the stored wrong attempt that feeds the cooldown would be lost. */
    @Transactional(noRollbackFor = WrongCodeException.class)
    public TeamProgress unlockDesktop(UUID teamId, UUID playerId, String caseId, String desktopId, String code) {
        TeamProgress progress = getProgress(teamId, playerId, caseId);
        Desktop desktop = caseCatalog.getDesktop(caseId, desktopId);
        if (!progress.isOpen(desktop)) {
            tryCode(teamId, caseId, UnlockTarget.desktop(desktopId), desktop.lock(), code);
        }
        return getProgress(teamId, playerId, caseId);
    }

    /** An item on a locked desktop can only be unlocked after the desktop itself. Wrong codes are kept. */
    @Transactional(noRollbackFor = WrongCodeException.class)
    public TeamProgress unlockItem(
            UUID teamId, UUID playerId, String caseId, String desktopId, String appId, String itemId, String code) {
        TeamProgress progress = getProgress(teamId, playerId, caseId);
        Desktop desktop = caseCatalog.getDesktop(caseId, desktopId);
        DesktopApp app =
                desktop.findApp(appId).orElseThrow(() -> new CaseContentNotFoundException("Unknown app: " + appId));
        LockableItem item =
                app.findItem(itemId).orElseThrow(() -> new CaseContentNotFoundException("Unknown item: " + itemId));
        if (!progress.isOpen(desktop)) {
            throw new CaseContentNotFoundException("Desktop is still locked: " + desktopId);
        }
        if (!progress.isOpen(desktop, app, item)) {
            tryCode(teamId, caseId, UnlockTarget.item(desktopId, appId, itemId), item.lock(), code);
        }
        return getProgress(teamId, playerId, caseId);
    }

    /** The cooldown is checked before the code, so a team waiting out a cooldown cannot keep guessing. */
    private void tryCode(UUID teamId, String caseId, UnlockTarget target, Lock lock, String code) {
        Instant now = clock.instant();
        requireNoCooldown(teamId, caseId, target, now);
        if (!lock.opensWith(code)) {
            wrongCodeAttemptRepository.save(new WrongCodeAttempt(teamId, caseId, target.key(), now));
            throw new WrongCodeException(target.key());
        }
        unlock(teamId, caseId, target);
    }

    private void requireNoCooldown(UUID teamId, String caseId, UnlockTarget target, Instant now) {
        List<WrongCodeAttempt> recentWrongAttempts =
                wrongCodeAttemptRepository.findByTeamIdAndCaseIdAndTargetAndAttemptedAtAfterOrderByAttemptedAtAsc(
                        teamId, caseId, target.key(), now.minus(wrongAttemptWindow));
        if (recentWrongAttempts.size() < maxWrongAttempts) {
            return;
        }
        // Guessing is possible again once enough of the recent wrong attempts have left the window
        Instant oldestCounted = recentWrongAttempts
                .get(recentWrongAttempts.size() - maxWrongAttempts)
                .getAttemptedAt();
        Duration retryAfter = Duration.between(now, oldestCounted.plus(wrongAttemptWindow));
        throw new CodeCooldownException(target.key(), retryAfter);
    }

    private boolean unlock(UUID teamId, String caseId, UnlockTarget target) {
        return teamUnlockRepository.insertIfAbsent(UUID.randomUUID(), teamId, caseId, target.key(), clock.instant())
                == 1;
    }
}
