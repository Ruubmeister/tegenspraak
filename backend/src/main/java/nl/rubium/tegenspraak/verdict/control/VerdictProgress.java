package nl.rubium.tegenspraak.verdict.control;

import java.util.List;
import nl.rubium.tegenspraak.casefile.entity.CaseSolution;
import nl.rubium.tegenspraak.verdict.entity.VerdictAttempt;

/** A team's attempts at solving a case, oldest first. A team that used all attempts without solving it has failed. */
public record VerdictProgress(CaseSolution solution, List<VerdictAttempt> attempts, int maxAttempts) {

    public VerdictProgress {
        attempts = List.copyOf(attempts);
    }

    public boolean solved() {
        return attempts.stream().anyMatch(VerdictAttempt::isSolved);
    }

    public boolean failed() {
        return !solved() && attempts.size() >= maxAttempts;
    }

    /** Solved and failed cases are over; the epilogue may then be shown. */
    public boolean finished() {
        return solved() || failed();
    }

    public int attemptsLeft() {
        return finished() ? 0 : maxAttempts - attempts.size();
    }
}
