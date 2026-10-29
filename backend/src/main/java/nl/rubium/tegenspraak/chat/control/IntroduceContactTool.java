package nl.rubium.tegenspraak.chat.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * Tool the character's LLM calls when it puts the team in touch with a hidden character. One instance per message: it
 * only collects the requests, the caller stores them once the reply succeeded, so a failed LLM call unlocks nothing.
 */
final class IntroduceContactTool implements Function<IntroduceContactTool.Request, String> {

    static final String NAME = "introduce_contact";

    record Request(String characterId) {}

    private final Map<String, CharacterProfile> introducibleById;
    private final List<CharacterProfile> introduced = new ArrayList<>();

    IntroduceContactTool(List<CharacterProfile> introducible) {
        this.introducibleById =
                introducible.stream().collect(Collectors.toMap(CharacterProfile::id, character -> character));
    }

    /** Without anyone to introduce, the tool is not offered to the LLM. */
    boolean canIntroduce() {
        return !introducibleById.isEmpty();
    }

    ToolCallback toCallback() {
        return FunctionToolCallback.builder(NAME, this)
                .description("Bring de rechercheurs in contact met een persoon die je kent, zodat ze die persoon"
                        + " kunnen chatten. Gebruik dit alleen als aan de voorwaarde voor die persoon is voldaan."
                        + " Geef het id van de persoon mee.")
                .inputType(Request.class)
                .build();
    }

    /** The result text goes back to the LLM, which then writes its reply. */
    @Override
    public synchronized String apply(Request request) {
        CharacterProfile character = request == null ? null : introducibleById.get(request.characterId());
        if (character == null) {
            return "Onbekend id. Je kunt alleen deze personen voorstellen: "
                    + String.join(", ", introducibleById.keySet());
        }
        if (!introduced.contains(character)) {
            introduced.add(character);
        }
        return "De rechercheurs kunnen nu contact opnemen met " + character.name() + ".";
    }

    synchronized List<CharacterProfile> introduced() {
        return List.copyOf(introduced);
    }
}
