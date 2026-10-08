package nl.rubium.tegenspraak.verdict.control;

import java.util.List;
import java.util.Optional;
import nl.rubium.tegenspraak.casefile.entity.CaseSolution;
import nl.rubium.tegenspraak.verdict.entity.VerdictAttempt;

/** A team's attempts at solving a case, oldest first. */
public record VerdictProgress(CaseSolution solution, List<VerdictAttempt> attempts) {

    public VerdictProgress {
        attempts = List.copyOf(attempts);
    }

    public boolean solved() {
        return attempts.stream().anyMatch(VerdictAttempt::isSolved);
    }

    public Optional<VerdictAttempt> latestAttempt() {
        return attempts.isEmpty() ? Optional.empty() : Optional.of(attempts.getLast());
    }
}
