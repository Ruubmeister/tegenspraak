package nl.rubium.tegenspraak.team.control;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JoinCodeGeneratorTest {

    private final JoinCodeGenerator joinCodeGenerator = new JoinCodeGenerator();

    @Test
    void generate_givenManyCodes_expectEightCharactersWithoutLookAlikes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(joinCodeGenerator.generate());
        }

        assertThat(codes).hasSize(1000).allSatisfy(code -> assertThat(code).matches("[A-HJ-NP-Z2-9]{8}"));
    }

    @Test
    void normalize_givenLowercaseWithDashAndSpaces_expectStoredForm() {
        assertThat(JoinCodeGenerator.normalize(" abcd-efgh ")).isEqualTo("ABCDEFGH");
    }

    @Test
    void format_givenStoredCode_expectDashInTheMiddle() {
        assertThat(JoinCodeGenerator.format("ABCDEFGH")).isEqualTo("ABCD-EFGH");
    }
}
