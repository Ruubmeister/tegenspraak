package nl.rubium.tegenspraak.casefile.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The questions a team must answer correctly to solve the case, and the epilogue shown afterwards. */
public record CaseSolution(
        @NotEmpty @Valid List<SolutionQuestion> questions,
        @NotBlank String epilogue) {

    public CaseSolution {
        questions = questions == null ? List.of() : List.copyOf(questions);
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
