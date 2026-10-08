package nl.rubium.tegenspraak.casefile.entity;

public record SolutionOption(String id, String label) {

    public SolutionOption {
        if (id == null || id.isBlank() || label == null || label.isBlank()) {
            throw new IllegalArgumentException("A solution option needs an id and a label");
        }
    }
}
