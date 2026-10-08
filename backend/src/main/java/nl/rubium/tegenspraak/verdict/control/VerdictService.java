package nl.rubium.tegenspraak.verdict.control;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.rubium.tegenspraak.casefile.control.CaseCatalog;
import nl.rubium.tegenspraak.casefile.entity.CaseSolution;
import nl.rubium.tegenspraak.casefile.entity.SolutionQuestion;
import nl.rubium.tegenspraak.team.control.TeamService;
import nl.rubium.tegenspraak.verdict.entity.VerdictAttempt;
import nl.rubium.tegenspraak.verdict.entity.VerdictAttemptRepository;
import org.springframework.stereotype.Service;

/**
 * Grades a team's answers to the solution questions of a case. Any member can submit; the case is solved for the whole
 * team when all answers are correct.
 */
@Service
public class VerdictService {

    private final CaseCatalog caseCatalog;
    private final VerdictAttemptRepository verdictAttemptRepository;
    private final TeamService teamService;

    public VerdictService(
            CaseCatalog caseCatalog, VerdictAttemptRepository verdictAttemptRepository, TeamService teamService) {
        this.caseCatalog = caseCatalog;
        this.verdictAttemptRepository = verdictAttemptRepository;
        this.teamService = teamService;
    }

    public VerdictProgress getProgress(UUID teamId, UUID playerId, String caseId) {
        teamService.requireMember(teamId, playerId);
        return loadProgress(teamId, caseId);
    }

    /** Stores the score of this attempt and returns the progress including it. */
    public VerdictProgress submit(
            UUID teamId, UUID playerId, String playerName, String caseId, Map<String, String> answersByQuestionId) {
        VerdictProgress progress = getProgress(teamId, playerId, caseId);
        if (progress.solved()) {
            throw new CaseAlreadySolvedException("Case " + caseId + " is already solved");
        }
        CaseSolution solution = progress.solution();
        requireAnswerForEveryQuestion(solution, answersByQuestionId);

        int correctAnswers = solution.countCorrectAnswers(answersByQuestionId);
        verdictAttemptRepository.save(new VerdictAttempt(
                teamId, caseId, playerName, correctAnswers, solution.questions().size()));
        return loadProgress(teamId, caseId);
    }

    public List<String> getSolvedCaseIds(UUID teamId, UUID playerId) {
        teamService.requireMember(teamId, playerId);
        return verdictAttemptRepository.findSolvedCaseIds(teamId);
    }

    private VerdictProgress loadProgress(UUID teamId, String caseId) {
        CaseSolution solution = caseCatalog.getCase(caseId).manifest().solution();
        return new VerdictProgress(
                solution, verdictAttemptRepository.findByTeamIdAndCaseIdOrderBySubmittedAtAsc(teamId, caseId));
    }

    private static void requireAnswerForEveryQuestion(CaseSolution solution, Map<String, String> answersByQuestionId) {
        if (answersByQuestionId.size() != solution.questions().size()) {
            throw new InvalidVerdictException("Expected one answer per question");
        }
        for (SolutionQuestion question : solution.questions()) {
            if (!question.hasOption(answersByQuestionId.get(question.id()))) {
                throw new InvalidVerdictException("No valid answer for question " + question.id());
            }
        }
    }
}
