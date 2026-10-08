package nl.rubium.tegenspraak.casefile.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The questions a player must answer correctly to solve the case, and the epilogue shown once they do. */
public record CaseSolution(List<SolutionQuestion> questions, String epilogue) {

    public CaseSolution {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("A solution needs at least one question");
        }
        if (epilogue == null || epilogue.isBlank()) {
            throw new IllegalArgumentException("A solution needs an epilogue");
        }
        questions = List.copyOf(questions);
        requireUniqueQuestionIds(questions);
    }

    /** Answers are keyed by question id; a missing answer counts as wrong. */
    public int countCorrectAnswers(Map<String, String> answersByQuestionId) {
        return (int) questions.stream()
                .filter(question -> question.isAnsweredCorrectly(answersByQuestionId.get(question.id())))
                .count();
    }

    private static void requireUniqueQuestionIds(List<SolutionQuestion> questions) {
        Set<String> seenIds = new HashSet<>();
        for (SolutionQuestion question : questions) {
            if (!seenIds.add(question.id())) {
                throw new IllegalArgumentException("Duplicate question id '" + question.id() + "'");
            }
        }
    }
}
