package nl.rubium.tegenspraak.investigation.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import nl.rubium.tegenspraak.casefile.control.CaseCatalog;
import nl.rubium.tegenspraak.casefile.control.CaseContentNotFoundException;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseFixtures;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.ContactIntroduction;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.FileEntry;
import nl.rubium.tegenspraak.desktop.entity.FilesApp;
import nl.rubium.tegenspraak.desktop.entity.Lock;
import nl.rubium.tegenspraak.desktop.entity.OwnerType;
import nl.rubium.tegenspraak.investigation.entity.TeamUnlockRepository;
import nl.rubium.tegenspraak.investigation.entity.WrongCodeAttempt;
import nl.rubium.tegenspraak.investigation.entity.WrongCodeAttemptRepository;
import nl.rubium.tegenspraak.team.control.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvestigationServiceTest {

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID PLAYER_ID = UUID.randomUUID();
    private static final String CASE_ID = "demo";
    private static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    private static final String SAFE_TARGET = "item:computer/bestanden/kluis";

    private static final CharacterProfile GARDENER = new CharacterProfile(
            "de-tuinman", "De Tuinman", "Tuinman", "In de kas.", null, null, null, null, true, null);
    private static final CharacterProfile BUTLER = new CharacterProfile(
            "de-butler",
            "De Butler",
            "Butler",
            "Altijd aanwezig.",
            null,
            null,
            null,
            null,
            false,
            List.of(new ContactIntroduction("de-tuinman", "De speler vraagt naar de tuin.")));
    private static final Desktop COMPUTER = new Desktop(
            "computer",
            "Landhuis",
            OwnerType.ORGANISATION,
            "Computer in de hal.",
            new Lock("1234", "Het jaartal"),
            List.of(new FilesApp(
                    "bestanden",
                    "Bestanden",
                    List.of(new FileEntry(
                            "kluis", "Prive", "kluis.txt", "Het testament.", new Lock("kandelaar", "Wapen"))))));
    private static final CaseDefinition CASE_DEFINITION = new CaseDefinition(
            CASE_ID, CaseFixtures.manifestWithSetting("Geheim"), List.of(BUTLER, GARDENER), List.of(COMPUTER));

    private final CaseCatalog caseCatalog = mock(CaseCatalog.class);
    private final TeamService teamService = mock(TeamService.class);
    private final TeamUnlockRepository teamUnlockRepository = mock(TeamUnlockRepository.class);
    private final WrongCodeAttemptRepository wrongCodeAttemptRepository = mock(WrongCodeAttemptRepository.class);
    private final InvestigationService investigationService = new InvestigationService(
            caseCatalog,
            teamService,
            teamUnlockRepository,
            wrongCodeAttemptRepository,
            Clock.fixed(NOW, ZoneOffset.UTC),
            5,
            Duration.ofMinutes(1));
    private final List<String> unlockedTargets = new ArrayList<>();
    private final List<WrongCodeAttempt> recentWrongAttempts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(caseCatalog.getCase(CASE_ID)).thenReturn(CASE_DEFINITION);
        when(caseCatalog.getDesktop(CASE_ID, "computer")).thenReturn(COMPUTER);
        when(caseCatalog.getCharacter(CASE_ID, "de-tuinman")).thenReturn(GARDENER);
        when(teamUnlockRepository.findTargets(TEAM_ID, CASE_ID)).thenAnswer(invocation -> List.copyOf(unlockedTargets));
        when(teamUnlockRepository.insertIfAbsent(any(), eq(TEAM_ID), eq(CASE_ID), anyString(), eq(NOW)))
                .thenAnswer(invocation -> {
                    String target = invocation.getArgument(3);
                    if (unlockedTargets.contains(target)) {
                        return 0;
                    }
                    unlockedTargets.add(target);
                    return 1;
                });
        when(wrongCodeAttemptRepository.findByTeamIdAndCaseIdAndTargetAndAttemptedAtAfterOrderByAttemptedAtAsc(
                        TEAM_ID, CASE_ID, SAFE_TARGET, NOW.minus(Duration.ofMinutes(1))))
                .thenAnswer(invocation -> List.copyOf(recentWrongAttempts));
    }

    @Test
    void requireAvailableCharacter_givenHiddenCharacterNotIntroduced_expectNotFound() {
        assertThatThrownBy(
                        () -> investigationService.requireAvailableCharacter(TEAM_ID, PLAYER_ID, CASE_ID, "de-tuinman"))
                .isInstanceOf(CaseContentNotFoundException.class);
    }

    @Test
    void requireAvailableCharacter_givenHiddenCharacterIntroduced_expectCharacter() {
        unlockedTargets.add("character:de-tuinman");

        assertThat(investigationService.requireAvailableCharacter(TEAM_ID, PLAYER_ID, CASE_ID, "de-tuinman"))
                .isEqualTo(GARDENER);
    }

    @Test
    void introduceCharacters_givenOneAlreadyIntroduced_expectOnlyNewOnesReturned() {
        unlockedTargets.add("character:de-tuinman");

        assertThat(investigationService.introduceCharacters(TEAM_ID, CASE_ID, List.of(GARDENER)))
                .isEmpty();
    }

    @Test
    void getProgress_givenNothingUnlocked_expectHiddenCharacterAndLocksClosed() {
        TeamProgress progress = investigationService.getProgress(TEAM_ID, PLAYER_ID, CASE_ID);

        assertThat(progress.availableCharacters()).containsExactly(BUTLER);
        assertThat(progress.introducibleBy(BUTLER)).containsExactly(GARDENER);
        assertThat(progress.isOpen(COMPUTER)).isFalse();
        verify(teamService).requireMember(TEAM_ID, PLAYER_ID);
    }

    @Test
    void unlockDesktop_givenCorrectCode_expectDesktopOpen() {
        TeamProgress progress = investigationService.unlockDesktop(TEAM_ID, PLAYER_ID, CASE_ID, "computer", " 1234 ");

        assertThat(progress.isOpen(COMPUTER)).isTrue();
    }

    @Test
    void unlockDesktop_givenWrongCode_expectWrongCodeAndAttemptStored() {
        assertThatThrownBy(() -> investigationService.unlockDesktop(TEAM_ID, PLAYER_ID, CASE_ID, "computer", "0000"))
                .isInstanceOf(WrongCodeException.class);

        verify(wrongCodeAttemptRepository).save(any(WrongCodeAttempt.class));
    }

    @Test
    void unlockItem_givenDesktopStillLocked_expectNotFound() {
        assertThatThrownBy(() -> investigationService.unlockItem(
                        TEAM_ID, PLAYER_ID, CASE_ID, "computer", "bestanden", "kluis", "kandelaar"))
                .isInstanceOf(CaseContentNotFoundException.class);
    }

    @Test
    void unlockItem_givenCorrectCodeOnOpenDesktop_expectItemOpen() {
        unlockedTargets.add("desktop:computer");

        investigationService.unlockItem(TEAM_ID, PLAYER_ID, CASE_ID, "computer", "bestanden", "kluis", "Kandelaar");

        assertThat(unlockedTargets).contains(SAFE_TARGET);
    }

    @Test
    void unlockItem_givenFourRecentWrongAttempts_expectAnotherAttemptAllowed() {
        unlockedTargets.add("desktop:computer");
        givenRecentWrongAttempts(4);

        assertThatThrownBy(() -> investigationService.unlockItem(
                        TEAM_ID, PLAYER_ID, CASE_ID, "computer", "bestanden", "kluis", "touw"))
                .isInstanceOf(WrongCodeException.class);
    }

    @Test
    void unlockItem_givenFiveRecentWrongAttempts_expectCooldownUntilOldestLeavesWindowEvenForCorrectCode() {
        unlockedTargets.add("desktop:computer");
        givenRecentWrongAttempts(5);

        assertThatThrownBy(() -> investigationService.unlockItem(
                        TEAM_ID, PLAYER_ID, CASE_ID, "computer", "bestanden", "kluis", "kandelaar"))
                .isInstanceOfSatisfying(
                        CodeCooldownException.class,
                        exception -> assertThat(exception.getRetryAfter()).isEqualTo(Duration.ofSeconds(10)));
        assertThat(unlockedTargets).doesNotContain(SAFE_TARGET);
        verify(wrongCodeAttemptRepository, never()).save(any(WrongCodeAttempt.class));
    }

    @Test
    void unlockItem_givenUnknownItem_expectNotFound() {
        unlockedTargets.add("desktop:computer");

        assertThatThrownBy(() -> investigationService.unlockItem(
                        TEAM_ID, PLAYER_ID, CASE_ID, "computer", "bestanden", "bestaat-niet", "1234"))
                .isInstanceOf(CaseContentNotFoundException.class);
    }

    /** Attempts 50, 40, 30... seconds ago; the oldest leaves the one minute window in 10 seconds. */
    private void givenRecentWrongAttempts(int count) {
        IntStream.range(0, count)
                .mapToObj(index ->
                        new WrongCodeAttempt(TEAM_ID, CASE_ID, SAFE_TARGET, NOW.minusSeconds(50 - index * 10L)))
                .forEach(recentWrongAttempts::add);
    }
}
