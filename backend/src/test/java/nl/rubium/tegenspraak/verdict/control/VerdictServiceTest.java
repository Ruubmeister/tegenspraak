package nl.rubium.tegenspraak.verdict.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import nl.rubium.tegenspraak.casefile.control.CaseCatalog;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseDifficulty;
import nl.rubium.tegenspraak.casefile.entity.CaseManifest;
import nl.rubium.tegenspraak.casefile.entity.CaseSolution;
import nl.rubium.tegenspraak.casefile.entity.SolutionQuestion;
import nl.rubium.tegenspraak.liveupdate.entity.TeamEvent;
import nl.rubium.tegenspraak.team.control.TeamNotFoundException;
import nl.rubium.tegenspraak.team.control.TeamService;
import nl.rubium.tegenspraak.verdict.entity.VerdictAnswer;
import nl.rubium.tegenspraak.verdict.entity.VerdictAttempt;
import nl.rubium.tegenspraak.verdict.entity.VerdictAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

class VerdictServiceTest {

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID PLAYER_ID = UUID.randomUUID();
    private static final String CASE_ID = "demo";
    private static final int MAX_ATTEMPTS = 3;
    private static final Map<String, String> ANSWERS = Map.of("dader", "De butler", "wapen", "Een kandelaar");

    private final CaseCatalog caseCatalog = mock(CaseCatalog.class);
    private final TeamService teamService = mock(TeamService.class);
    private final VerdictAttemptRepository verdictAttemptRepository = mock(VerdictAttemptRepository.class);
    private final VerdictGrader verdictGrader = mock(VerdictGrader.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final VerdictService verdictService = new VerdictService(
            caseCatalog,
            verdictAttemptRepository,
            teamService,
            verdictGrader,
            eventPublisher,
            new VerdictProperties(MAX_ATTEMPTS));
    private final List<VerdictAttempt> storedAttempts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CaseSolution solution = new CaseSolution(
                List.of(
                        new SolutionQuestion("dader", "Wie deed het?", "De butler."),
                        new SolutionQuestion("wapen", "Waarmee?", "Met de kandelaar.")),
                "De butler bekende alles.");
        CaseDefinition caseDefinition = new CaseDefinition(
                CASE_ID,
                new CaseManifest("Demo", "Kort", CaseDifficulty.MAKKELIJK, "Intro", "Geheim", solution),
                List.of(),
                List.of());
        when(caseCatalog.getCase(CASE_ID)).thenReturn(caseDefinition);
        when(verdictAttemptRepository.findByTeamIdAndCaseIdOrderByAttemptNumberAsc(TEAM_ID, CASE_ID))
                .thenAnswer(invocation -> List.copyOf(storedAttempts));
        when(verdictAttemptRepository.saveAndFlush(any(VerdictAttempt.class))).thenAnswer(invocation -> {
            VerdictAttempt attempt = invocation.getArgument(0);
            storedAttempts.add(attempt);
            return attempt;
        });
    }

    @Test
    void submit_givenAllAnswersGradedCorrect_expectCaseSolved() {
        givenGraderMarksCorrect("dader", "wapen");

        VerdictProgress progress = verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, ANSWERS);

        assertThat(progress.solved()).isTrue();
        assertThat(progress.attemptsLeft()).isZero();
        assertThat(progress.attempts()).singleElement().satisfies(attempt -> {
            assertThat(attempt.getCorrectAnswers()).isEqualTo(2);
            assertThat(attempt.getTotalQuestions()).isEqualTo(2);
        });
    }

    @Test
    void submit_givenAttemptStored_expectVerdictEventPublished() {
        givenGraderMarksCorrect("dader");

        verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, ANSWERS);

        verify(eventPublisher).publishEvent(TeamEvent.verdict(TEAM_ID, CASE_ID));
    }

    @Test
    void submit_givenOneAnswerGradedWrong_expectScoreAndAnswersStoredAndCaseNotSolved() {
        givenGraderMarksCorrect("dader");

        VerdictProgress progress = verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, ANSWERS);

        assertThat(progress.solved()).isFalse();
        assertThat(progress.attemptsLeft()).isEqualTo(MAX_ATTEMPTS - 1);
        assertThat(progress.attempts()).singleElement().satisfies(attempt -> {
            assertThat(attempt.getCorrectAnswers()).isEqualTo(1);
            assertThat(attempt.getAttemptNumber()).isEqualTo(1);
            assertThat(attempt.getAnswers())
                    .containsExactly(
                            new VerdictAnswer("dader", "De butler", true),
                            new VerdictAnswer("wapen", "Een kandelaar", false));
        });
    }

    @Test
    void submit_givenAnswersWithSurroundingWhitespace_expectStrippedAnswersGraded() {
        givenGraderMarksCorrect("dader", "wapen");

        verdictService.submit(
                TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "  De butler\n", "wapen", " Een kandelaar "));

        verify(verdictGrader).gradeCorrectQuestionIds(any(CaseSolution.class), any());
        assertThat(storedAttempts.getFirst().getAnswers())
                .extracting(VerdictAnswer::answer)
                .containsExactly("De butler", "Een kandelaar");
    }

    @Test
    void submit_givenWrongAttemptThenCorrectAttempt_expectSolvedAfterTwoNumberedAttempts() {
        givenGraderMarksCorrect();
        verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, ANSWERS);
        givenGraderMarksCorrect("dader", "wapen");

        VerdictProgress progress = verdictService.submit(TEAM_ID, PLAYER_ID, "bram", CASE_ID, ANSWERS);

        assertThat(progress.solved()).isTrue();
        assertThat(progress.attempts())
                .extracting(VerdictAttempt::getAttemptNumber)
                .containsExactly(1, 2);
    }

    @Test
    void submit_givenLastAttemptWrong_expectCaseFailed() {
        givenGraderMarksCorrect("dader");

        VerdictProgress progress = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            progress = verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, ANSWERS);
        }

        assertThat(progress.failed()).isTrue();
        assertThat(progress.finished()).isTrue();
        assertThat(progress.attemptsLeft()).isZero();
    }

    @Test
    void submit_givenNoAttemptsLeft_expectNoAttemptsLeftAndGraderNotCalled() {
        givenGraderMarksCorrect();
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, ANSWERS);
        }

        assertThatThrownBy(() -> verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, ANSWERS))
                .isInstanceOf(NoAttemptsLeftException.class);
        verify(verdictGrader, times(MAX_ATTEMPTS)).gradeCorrectQuestionIds(any(), anyMap());
    }

    @Test
    void submit_givenGradingFails_expectGradingUnavailableAndNoAttemptUsed() {
        when(verdictGrader.gradeCorrectQuestionIds(any(), anyMap()))
                .thenThrow(new VerdictGradingUnavailableException("LLM down"));

        assertThatThrownBy(() -> verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, ANSWERS))
                .isInstanceOf(VerdictGradingUnavailableException.class);

        verify(verdictAttemptRepository, never()).saveAndFlush(any());
        assertThat(verdictService.getProgress(TEAM_ID, PLAYER_ID, CASE_ID).attemptsLeft())
                .isEqualTo(MAX_ATTEMPTS);
    }

    @Test
    void submit_givenTeammateTookAttemptNumberDuringGrading_expectConcurrentAttempt() {
        givenGraderMarksCorrect("dader");
        when(verdictAttemptRepository.saveAndFlush(any(VerdictAttempt.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate attempt number"));

        assertThatThrownBy(() -> verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, ANSWERS))
                .isInstanceOf(ConcurrentVerdictAttemptException.class);
    }

    @Test
    void submit_givenMissingAnswer_expectInvalidVerdictAndGraderNotCalled() {
        assertThatThrownBy(
                        () -> verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "De butler")))
                .isInstanceOf(InvalidVerdictException.class);

        verify(verdictGrader, never()).gradeCorrectQuestionIds(any(), anyMap());
        verify(verdictAttemptRepository, never()).saveAndFlush(any());
    }

    @Test
    void submit_givenBlankAnswer_expectInvalidVerdict() {
        assertThatThrownBy(() -> verdictService.submit(
                        TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "De butler", "wapen", "   ")))
                .isInstanceOf(InvalidVerdictException.class);
    }

    @Test
    void submit_givenUnknownQuestionInsteadOfKnownOne_expectInvalidVerdict() {
        assertThatThrownBy(() -> verdictService.submit(
                        TEAM_ID, PLAYER_ID, "anouk", CASE_ID, Map.of("dader", "De butler", "motief", "Geld")))
                .isInstanceOf(InvalidVerdictException.class);
    }

    @Test
    void submit_givenCaseAlreadySolved_expectCaseAlreadySolved() {
        givenGraderMarksCorrect("dader", "wapen");
        verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, ANSWERS);

        assertThatThrownBy(() -> verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, ANSWERS))
                .isInstanceOf(CaseAlreadySolvedException.class);
    }

    @Test
    void submit_givenAttempt_expectSubmitterStored() {
        givenGraderMarksCorrect();

        verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, ANSWERS);

        assertThat(storedAttempts).singleElement().satisfies(attempt -> {
            assertThat(attempt.getTeamId()).isEqualTo(TEAM_ID);
            assertThat(attempt.getSubmittedBy()).isEqualTo("anouk");
        });
    }

    @Test
    void submit_givenPlayerNotInTeam_expectTeamNotFoundAndNothingStored() {
        doThrow(new TeamNotFoundException("Not a member")).when(teamService).requireMember(TEAM_ID, PLAYER_ID);

        assertThatThrownBy(() -> verdictService.submit(TEAM_ID, PLAYER_ID, "anouk", CASE_ID, ANSWERS))
                .isInstanceOf(TeamNotFoundException.class);
        verify(verdictAttemptRepository, never()).saveAndFlush(any(VerdictAttempt.class));
    }

    @Test
    void getProgress_givenNoAttempts_expectUnsolvedWithAllAttemptsLeft() {
        VerdictProgress progress = verdictService.getProgress(TEAM_ID, PLAYER_ID, CASE_ID);

        assertThat(progress.solved()).isFalse();
        assertThat(progress.failed()).isFalse();
        assertThat(progress.attempts()).isEmpty();
        assertThat(progress.attemptsLeft()).isEqualTo(MAX_ATTEMPTS);
    }

    private void givenGraderMarksCorrect(String... questionIds) {
        when(verdictGrader.gradeCorrectQuestionIds(any(CaseSolution.class), anyMap()))
                .thenReturn(Set.of(questionIds));
    }
}
