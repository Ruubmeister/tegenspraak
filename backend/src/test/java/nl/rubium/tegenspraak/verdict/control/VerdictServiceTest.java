package nl.rubium.tegenspraak.verdict.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.rubium.tegenspraak.casefile.control.CaseCatalog;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseManifest;
import nl.rubium.tegenspraak.casefile.entity.CaseSolution;
import nl.rubium.tegenspraak.casefile.entity.SolutionOption;
import nl.rubium.tegenspraak.casefile.entity.SolutionQuestion;
import nl.rubium.tegenspraak.team.control.TeamNotFoundException;
import nl.rubium.tegenspraak.team.control.TeamService;
import nl.rubium.tegenspraak.verdict.entity.VerdictAttempt;
import nl.rubium.tegenspraak.verdict.entity.VerdictAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VerdictServiceTest {

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID PLAYER_ID = UUID.randomUUID();
    private static final String CASE_ID = "demo";

    private final CaseCatalog caseCatalog = mock(CaseCatalog.class);
    private final TeamService teamService = mock(TeamService.class);
    private final VerdictAttemptRepository verdictAttemptRepository = mock(VerdictAttemptRepository.class);
    private final VerdictService verdictService =
            new VerdictService(caseCatalog, verdictAttemptRepository, teamService);
    private final List<VerdictAttempt> storedAttempts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CaseSolution solution = new CaseSolution(
                List.of(question("dader", "de-butler", "de-tuinman"), question("wapen", "kandelaar", "touw")),
                "De butler bekende alles.");
        CaseDefinition caseDefinition = new CaseDefinition(
                CASE_ID, new CaseManifest("Demo", "Kort", "Intro", "Geheim", solution), List.of(), List.of());
        when(caseCatalog.getCase(CASE_ID)).thenReturn(caseDefinition);
        when(verdictAttemptRepository.findByTeamIdAndCaseIdOrderBySubmittedAtAsc(TEAM_ID, CASE_ID))
                .thenAnswer(invocation -> List.copyOf(storedAttempts));
        when(verdictAttemptRepository.save(any(VerdictAttempt.class))).thenAnswer(invocation -> {
            VerdictAttempt attempt = invocation.getArgument(0);
            storedAttempts.add(attempt);
            return attempt;
        });
    }

    @Test
    void submit_givenAllAnswersCorrect_expectCaseSolved() {
        VerdictProgress progress = verdictService.submit(
                TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "de-butler", "wapen", "kandelaar"));

        assertThat(progress.solved()).isTrue();
        assertThat(progress.latestAttempt()).hasValueSatisfying(attempt -> {
            assertThat(attempt.getCorrectAnswers()).isEqualTo(2);
            assertThat(attempt.getTotalQuestions()).isEqualTo(2);
        });
    }

    @Test
    void submit_givenOneWrongAnswer_expectScoreStoredAndCaseNotSolved() {
        VerdictProgress progress = verdictService.submit(
                TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "de-butler", "wapen", "touw"));

        assertThat(progress.solved()).isFalse();
        assertThat(progress.attempts())
                .singleElement()
                .satisfies(attempt -> assertThat(attempt.getCorrectAnswers()).isEqualTo(1));
    }

    @Test
    void submit_givenWrongAttemptThenCorrectAttempt_expectSolvedAfterTwoAttempts() {
        verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "de-tuinman", "wapen", "touw"));

        VerdictProgress progress = verdictService.submit(
                TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "de-butler", "wapen", "kandelaar"));

        assertThat(progress.solved()).isTrue();
        assertThat(progress.attempts()).hasSize(2);
    }

    @Test
    void submit_givenMissingAnswer_expectInvalidVerdictAndNothingStored() {
        assertThatThrownBy(
                        () -> verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "de-butler")))
                .isInstanceOf(InvalidVerdictException.class);

        verify(verdictAttemptRepository, never()).save(any());
    }

    @Test
    void submit_givenUnknownOption_expectInvalidVerdict() {
        assertThatThrownBy(() -> verdictService.submit(
                        TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "de-kok", "wapen", "kandelaar")))
                .isInstanceOf(InvalidVerdictException.class);
    }

    @Test
    void submit_givenUnknownQuestionInsteadOfKnownOne_expectInvalidVerdict() {
        assertThatThrownBy(() -> verdictService.submit(
                        TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "de-butler", "motief", "kandelaar")))
                .isInstanceOf(InvalidVerdictException.class);
    }

    @Test
    void submit_givenCaseAlreadySolved_expectCaseAlreadySolved() {
        verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "de-butler", "wapen", "kandelaar"));

        assertThatThrownBy(() -> verdictService.submit(
                        TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "de-butler", "wapen", "kandelaar")))
                .isInstanceOf(CaseAlreadySolvedException.class);
    }

    @Test
    void getProgress_givenNoAttempts_expectUnsolvedWithoutLatestAttempt() {
        VerdictProgress progress = verdictService.getProgress(TEAM_ID, PLAYER_ID, CASE_ID);

        assertThat(progress.solved()).isFalse();
        assertThat(progress.latestAttempt()).isEmpty();
    }

    private static SolutionQuestion question(String id, String correctOption, String wrongOption) {
        return new SolutionQuestion(
                id,
                "Vraag " + id,
                List.of(new SolutionOption(correctOption, correctOption), new SolutionOption(wrongOption, wrongOption)),
                correctOption);
    }

    @Test
    void submit_givenAttempt_expectSubmitterStored() {
        verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "de-tuinman", "wapen", "touw"));

        assertThat(storedAttempts).singleElement().satisfies(attempt -> {
            assertThat(attempt.getTeamId()).isEqualTo(TEAM_ID);
            assertThat(attempt.getSubmittedBy()).isEqualTo("anouk");
        });
    }

    @Test
    void submit_givenPlayerNotInTeam_expectTeamNotFoundAndNothingStored() {
        doThrow(new TeamNotFoundException("Not a member")).when(teamService).requireMember(TEAM_ID, PLAYER_ID);

        assertThatThrownBy(() -> verdictService.submit(
                        TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "de-butler", "wapen", "kandelaar")))
                .isInstanceOf(TeamNotFoundException.class);
        verify(verdictAttemptRepository, never()).save(any(VerdictAttempt.class));
    }
}
