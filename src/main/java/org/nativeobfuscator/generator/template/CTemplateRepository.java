package org.nativeobfuscator.generator.template;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads immutable C templates from application resources and performs strict
 * named-placeholder substitution.
 */
public final class CTemplateRepository {
    private static final String ROOT = "native/templates/";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([A-Z0-9_]+)}}");

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final ClassLoader classLoader;

    public CTemplateRepository() {
        this(CTemplateRepository.class.getClassLoader());
    }

    CTemplateRepository(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public String load(String name) {
        return cache.computeIfAbsent(name, this::readTemplate);
    }

    public String render(String name, Map<String, ?> values) {
        String rendered = load(name);
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }

        Matcher unresolved = PLACEHOLDER.matcher(rendered);
        if (unresolved.find()) {
            throw new IllegalArgumentException(
                    "Unresolved placeholder " + unresolved.group() + " in template " + name);
        }
        return rendered;
    }

    private String readTemplate(String name) {
        String resource = ROOT + name;
        try (InputStream input = classLoader.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing C template resource: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new UncheckedIOException("Unable to read C template " + resource, error);
        }
    }
}
