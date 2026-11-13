package nl.rubium.tegenspraak.verdict.control;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import nl.rubium.tegenspraak.casefile.control.CaseCatalog;
import nl.rubium.tegenspraak.casefile.entity.CaseSolution;
import nl.rubium.tegenspraak.casefile.entity.SolutionQuestion;
import nl.rubium.tegenspraak.liveupdate.entity.TeamEvent;
import nl.rubium.tegenspraak.team.control.TeamService;
import nl.rubium.tegenspraak.verdict.entity.VerdictAnswer;
import nl.rubium.tegenspraak.verdict.entity.VerdictAttempt;
import nl.rubium.tegenspraak.verdict.entity.VerdictAttemptRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Grades a team's typed answers to the solution questions of a case. Any member can submit; the case is solved for the
 * whole team when all answers are correct, and failed once the team used all its attempts.
 */
@Service
public class VerdictService {

    private final CaseCatalog caseCatalog;
    private final VerdictAttemptRepository verdictAttemptRepository;
    private final TeamService teamService;
    private final VerdictGrader verdictGrader;
    private final ApplicationEventPublisher eventPublisher;
    private final int maxAttempts;

    public VerdictService(
            CaseCatalog caseCatalog,
            VerdictAttemptRepository verdictAttemptRepository,
            TeamService teamService,
            VerdictGrader verdictGrader,
            ApplicationEventPublisher eventPublisher,
            VerdictProperties verdictProperties) {
        this.caseCatalog = caseCatalog;
        this.verdictAttemptRepository = verdictAttemptRepository;
        this.teamService = teamService;
        this.verdictGrader = verdictGrader;
        this.eventPublisher = eventPublisher;
        this.maxAttempts = verdictProperties.maxAttempts();
    }

    public VerdictProgress getProgress(UUID teamId, UUID playerId, String caseId) {
        teamService.requireMember(teamId, playerId);
        return loadProgress(teamId, caseId);
    }

    /**
     * Grades the answers and stores the attempt. A failed grading stores nothing, so it does not cost an attempt.
     *
     * @param answersByQuestionId the typed answer per question id
     */
    public VerdictProgress submit(
            UUID teamId, UUID playerId, String playerName, String caseId, Map<String, String> answersByQuestionId) {
        VerdictProgress progress = getProgress(teamId, playerId, caseId);
        if (progress.solved()) {
            throw new CaseAlreadySolvedException("Case " + caseId + " is already solved");
        }
        if (progress.failed()) {
            throw new NoAttemptsLeftException("No attempts left for case " + caseId);
        }
        CaseSolution solution = progress.solution();
        Map<String, String> answers = requireAnswerForEveryQuestion(solution, answersByQuestionId);

        Set<String> correctQuestionIds = verdictGrader.gradeCorrectQuestionIds(solution, answers);
        List<VerdictAnswer> gradedAnswers = solution.questions().stream()
                .map(question -> new VerdictAnswer(
                        question.id(), answers.get(question.id()), correctQuestionIds.contains(question.id())))
                .toList();
        storeAttempt(new VerdictAttempt(teamId, caseId, progress.attempts().size() + 1, playerName, gradedAnswers));
        eventPublisher.publishEvent(TeamEvent.verdict(teamId, caseId));
        return loadProgress(teamId, caseId);
    }

    public List<String> getSolvedCaseIds(UUID teamId, UUID playerId) {
        teamService.requireMember(teamId, playerId);
        return verdictAttemptRepository.findSolvedCaseIds(teamId);
    }

    /** Grading takes seconds; a teammate who submitted meanwhile took this attempt number first. */
    private void storeAttempt(VerdictAttempt attempt) {
        try {
            verdictAttemptRepository.saveAndFlush(attempt);
        } catch (DataIntegrityViolationException e) {
            throw new ConcurrentVerdictAttemptException(
                    "Attempt " + attempt.getAttemptNumber() + " was already submitted", e);
        }
    }

    private VerdictProgress loadProgress(UUID teamId, String caseId) {
        CaseSolution solution = caseCatalog.getCase(caseId).manifest().solution();
        return new VerdictProgress(
                solution,
                verdictAttemptRepository.findByTeamIdAndCaseIdOrderByAttemptNumberAsc(teamId, caseId),
                maxAttempts);
    }

    /** Returns the answers stripped of surrounding whitespace. */
    private static Map<String, String> requireAnswerForEveryQuestion(
            CaseSolution solution, Map<String, String> answersByQuestionId) {
        if (answersByQuestionId.size() != solution.questions().size()) {
            throw new InvalidVerdictException("Expected one answer per question");
        }
        for (SolutionQuestion question : solution.questions()) {
            String answer = answersByQuestionId.get(question.id());
            if (answer == null || answer.isBlank()) {
                throw new InvalidVerdictException("No answer for question " + question.id());
            }
        }
        return answersByQuestionId.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().strip()));
    }
}
