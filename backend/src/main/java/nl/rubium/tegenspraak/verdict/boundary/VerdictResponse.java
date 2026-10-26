package nl.rubium.tegenspraak.verdict.boundary;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.rubium.tegenspraak.casefile.entity.SolutionQuestion;
import nl.rubium.tegenspraak.verdict.control.VerdictProgress;
import nl.rubium.tegenspraak.verdict.entity.VerdictAnswer;
import nl.rubium.tegenspraak.verdict.entity.VerdictAttempt;

/**
 * The questions without their answers, plus the team's attempts. An attempt only tells how many answers were correct,
 * not which ones. The epilogue reveals the truth, so it is only included once the case is solved or failed.
 */
public record VerdictResponse(
        List<QuestionResponse> questions,
        int maxAttempts,
        int attemptsLeft,
        boolean solved,
        boolean failed,
        List<AttemptResponse> attempts,
        String epilogue) {

    public record QuestionResponse(String id, String question) {

        static QuestionResponse from(SolutionQuestion question) {
            return new QuestionResponse(question.id(), question.question());
        }
    }

    /** {@code answers} holds the typed answer per question id. */
    public record AttemptResponse(
            int number,
            String submittedBy,
            Instant submittedAt,
            int correctAnswers,
            int totalQuestions,
            Map<String, String> answers) {

        static AttemptResponse from(VerdictAttempt attempt) {
            return new AttemptResponse(
                    attempt.getAttemptNumber(),
                    attempt.getSubmittedBy(),
                    attempt.getSubmittedAt(),
                    attempt.getCorrectAnswers(),
                    attempt.getTotalQuestions(),
                    attempt.getAnswers().stream()
                            .collect(Collectors.toMap(VerdictAnswer::questionId, VerdictAnswer::answer)));
        }
    }

    static VerdictResponse from(VerdictProgress progress) {
        return new VerdictResponse(
                progress.solution().questions().stream()
                        .map(QuestionResponse::from)
                        .toList(),
                progress.maxAttempts(),
                progress.attemptsLeft(),
                progress.solved(),
                progress.failed(),
                progress.attempts().stream().map(AttemptResponse::from).toList(),
                progress.finished() ? progress.solution().epilogue() : null);
    }
}
