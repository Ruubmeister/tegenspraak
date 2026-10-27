package nl.rubium.tegenspraak.chat.control;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.ContactConsultation;
import org.junit.jupiter.api.Test;

class AskContactToolTest {

    private static final CharacterProfile COOK = new CharacterProfile(
            "de-kok", "De Kok", "Kok", "In de keuken.", null, null, null, null, false, null, null, null);
    private static final ConsultationContact SAFE_CODE = new ConsultationContact(
            new ContactConsultation(
                    "kluiscode", "de-kok", "De kluis", "Als ze ernaar vragen.", "De code is 1234.", null, null),
            COOK);

    private final AskContactTool tool = new AskContactTool(List.of(SAFE_CODE));

    @Test
    void apply_givenKnownConsultation_expectQuestionCollectedAndWaitExplained() {
        String result = tool.apply(new AskContactTool.Request("kluiscode", "  Weet jij de code?  "));

        assertThat(result).contains("De Kok", "paar minuten");
        assertThat(tool.asked()).containsExactly(new AskContactTool.AskedQuestion(SAFE_CODE, "Weet jij de code?"));
    }

    @Test
    void apply_givenUnknownConsultation_expectNothingCollectedAndKnownIdsListed() {
        String result = tool.apply(new AskContactTool.Request("testament", "Waar is het?"));

        assertThat(result).contains("kluiscode");
        assertThat(tool.asked()).isEmpty();
    }

    @Test
    void apply_givenBlankQuestion_expectNothingCollected() {
        tool.apply(new AskContactTool.Request("kluiscode", " "));

        assertThat(tool.asked()).isEmpty();
    }

    @Test
    void apply_givenSameConsultationTwice_expectFirstQuestionKept() {
        tool.apply(new AskContactTool.Request("kluiscode", "Eerste vraag"));
        tool.apply(new AskContactTool.Request("kluiscode", "Tweede vraag"));

        assertThat(tool.asked())
                .extracting(AskContactTool.AskedQuestion::question)
                .containsExactly("Eerste vraag");
    }

    @Test
    void apply_givenVeryLongQuestion_expectQuestionTruncated() {
        tool.apply(new AskContactTool.Request("kluiscode", "a".repeat(5000)));

        assertThat(tool.asked().getFirst().question()).hasSize(AskContactTool.MAX_QUESTION_LENGTH);
    }
}
