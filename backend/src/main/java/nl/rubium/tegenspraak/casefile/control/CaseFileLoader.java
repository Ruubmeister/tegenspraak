package nl.rubium.tegenspraak.casefile.control;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseManifest;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.InjectableValues;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Reads cases from a directory tree:
 *
 * <pre>
 * {location}/{case-id}/case.yaml
 * {location}/{case-id}/characters/{character-id}.yaml
 * {location}/{case-id}/desktops/{desktop-id}.yaml
 * </pre>
 *
 * Folder and file names are the ids. Any invalid file fails startup, so broken case content is never served.
 */
@Component
public class CaseFileLoader {

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final String CASE_FILE = "case.yaml";

    private final ResourcePatternResolver resourceResolver;
    private final YAMLMapper yamlMapper = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public CaseFileLoader(ResourcePatternResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public List<CaseDefinition> loadAll(String location) {
        List<CaseDefinition> cases = new ArrayList<>();
        for (Resource manifestResource : findResources(location + "/*/" + CASE_FILE)) {
            String caseId = parentFolderName(manifestResource);
            cases.add(loadCase(location + "/" + caseId, caseId, manifestResource));
        }
        cases.sort(Comparator.comparing(CaseDefinition::id));
        return cases;
    }

    private CaseDefinition loadCase(String caseLocation, String caseId, Resource manifestResource) {
        requireValidId(caseId, manifestResource);
        CaseManifest manifest = read(manifestResource, CaseManifest.class, null);
        List<CharacterProfile> characters = loadEntries(caseLocation + "/characters/*.yaml", CharacterProfile.class);
        List<Desktop> desktops = loadEntries(caseLocation + "/desktops/*.yaml", Desktop.class);
        return new CaseDefinition(caseId, manifest, characters, desktops);
    }

    private <T> List<T> loadEntries(String pattern, Class<T> type) {
        List<T> entries = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (Resource resource : findResources(pattern)) {
            String id = fileNameWithoutExtension(resource);
            requireValidId(id, resource);
            if (!seenIds.add(id)) {
                throw new IllegalStateException("Duplicate id '" + id + "' in " + pattern);
            }
            entries.add(read(resource, type, id));
        }
        return entries;
    }

    private <T> T read(Resource resource, Class<T> type, String injectedId) {
        InjectableValues.Std injectables = new InjectableValues.Std().addValue("id", injectedId);
        try (InputStream input = resource.getInputStream()) {
            return yamlMapper.readerFor(type).with(injectables).readValue(input);
        } catch (IOException | JacksonException e) {
            throw new IllegalStateException("Invalid case file " + resource.getDescription(), e);
        }
    }

    private Resource[] findResources(String pattern) {
        try {
            Resource[] resources = resourceResolver.getResources(pattern);
            Arrays.sort(resources, Comparator.comparing(Resource::getFilename));
            return resources;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list case files for " + pattern, e);
        }
    }

    private static void requireValidId(String id, Resource resource) {
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalStateException(
                    "Invalid id '" + id + "' for " + resource.getDescription() + ", use lowercase kebab-case");
        }
    }

    private static String fileNameWithoutExtension(Resource resource) {
        String fileName = resource.getFilename();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    private static String parentFolderName(Resource resource) {
        try {
            String path = resource.getURI().toString();
            String folderPath = path.substring(0, path.lastIndexOf('/'));
            return folderPath.substring(folderPath.lastIndexOf('/') + 1);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot resolve case folder for " + resource.getDescription(), e);
        }
    }
}
