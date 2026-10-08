package nl.rubium.tegenspraak.verdict.boundary;

import java.util.List;
import nl.rubium.tegenspraak.casefile.entity.SolutionOption;
import nl.rubium.tegenspraak.casefile.entity.SolutionQuestion;
import nl.rubium.tegenspraak.verdict.control.VerdictProgress;
import nl.rubium.tegenspraak.verdict.entity.VerdictAttempt;

/**
 * The questions without their answers, plus the team's progress. The epilogue reveals the truth, so it is only
 * included once the case is solved.
 */
public record VerdictResponse(
        List<QuestionResponse> questions, int attempts, boolean solved, ScoreResponse lastScore, String epilogue) {

    public record QuestionResponse(String id, String question, List<OptionResponse> options) {

        static QuestionResponse from(SolutionQuestion question) {
            return new QuestionResponse(
                    question.id(),
                    question.question(),
                    question.options().stream().map(OptionResponse::from).toList());
        }
    }

    public record OptionResponse(String id, String label) {

        static OptionResponse from(SolutionOption option) {
            return new OptionResponse(option.id(), option.label());
        }
    }

    public record ScoreResponse(int correctAnswers, int totalQuestions, String submittedBy) {

        static ScoreResponse from(VerdictAttempt attempt) {
            return new ScoreResponse(
                    attempt.getCorrectAnswers(), attempt.getTotalQuestions(), attempt.getSubmittedBy());
        }
    }

    static VerdictResponse from(VerdictProgress progress) {
        boolean solved = progress.solved();
        return new VerdictResponse(
                progress.solution().questions().stream()
                        .map(QuestionResponse::from)
                        .toList(),
                progress.attempts().size(),
                solved,
                progress.latestAttempt().map(ScoreResponse::from).orElse(null),
                solved ? progress.solution().epilogue() : null);
    }
}
