package nl.rubium.tegenspraak.casefile.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The questions a team must answer correctly to solve the case, and the epilogue shown afterwards. */
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

    public Optional<SolutionQuestion> findQuestion(String questionId) {
        return questions.stream()
                .filter(question -> question.id().equals(questionId))
                .findFirst();
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
