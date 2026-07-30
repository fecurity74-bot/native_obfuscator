package org.nativeobfuscator;

import org.nativeobfuscator.utils.asm.ClassWrapper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Writes the output archive transactionally and guarantees that every normalized
 * entry name occurs at most once.
 */
public final class JarArchiveWriter {
    private static final String MANIFEST = "META-INF/MANIFEST.MF";

    private JarArchiveWriter() {
    }

    public static void write(File outputFile, Map<String, ClassWrapper> classes, Map<String, byte[]> resources)
            throws IOException {
        Path output = outputFile.toPath().toAbsolutePath().normalize();
        Path parent = output.getParent();
        if (parent == null) {
            throw new IOException("Output JAR has no parent directory: " + output);
        }
        Files.createDirectories(parent);

        TreeMap<String, byte[]> entries = new TreeMap<>();
        List<Map.Entry<String, ClassWrapper>> sortedClasses = new ArrayList<>(classes.entrySet());
        sortedClasses.sort(Map.Entry.comparingByKey());
        for (Map.Entry<String, ClassWrapper> entry : sortedClasses) {
            String name = normalizeEntryName(entry.getKey());
            if (!name.endsWith(".class")) {
                name += ".class";
            }
            byte[] previous = entries.putIfAbsent(name, entry.getValue().toByteArray());
            if (previous != null) {
                throw new IOException("Duplicate class entry after normalization: " + name);
            }
        }

        byte[] manifest = null;
        List<Map.Entry<String, byte[]>> sortedResources = new ArrayList<>(resources.entrySet());
        sortedResources.sort(Comparator.comparing(Map.Entry::getKey));
        for (Map.Entry<String, byte[]> entry : sortedResources) {
            String name = normalizeEntryName(entry.getKey());
            if (MANIFEST.equalsIgnoreCase(name)) {
                manifest = entry.getValue();
                continue;
            }
            if (isInvalidatedSignature(name)) {
                NativeObfuscator.getLogger().debug("Dropping invalidated JAR signature: {}", name);
                continue;
            }
            if (entries.containsKey(name)) {
                NativeObfuscator.getLogger().warn("Skipping resource that collides with generated class: {}", name);
                continue;
            }
            if (entries.putIfAbsent(name, entry.getValue()) != null) {
                NativeObfuscator.getLogger().warn("Skipping duplicate normalized resource entry: {}", name);
            }
        }

        Path temporary = Files.createTempFile(parent, outputFile.getName() + ".", ".tmp");
        boolean moved = false;
        try {
            try (JarOutputStream jar = new JarOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                if (manifest != null) {
                    writeEntry(jar, MANIFEST, manifest);
                }
                for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    writeEntry(jar, entry.getKey(), entry.getValue());
                }
            }
            try {
                Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    public static String normalizeEntryName(String rawName) throws IOException {
        if (rawName == null || rawName.isBlank()) {
            throw new IOException("Empty JAR entry name");
        }
        String name = rawName.replace('\\', '/');
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (name.isBlank()) {
            throw new IOException("Empty JAR entry name");
        }
        String[] parts = name.split("/");
        StringBuilder normalized = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                throw new IOException("Unsafe JAR entry name: " + rawName);
            }
            if (normalized.length() > 0) {
                normalized.append('/');
            }
            normalized.append(part);
        }
        if (normalized.length() == 0) {
            throw new IOException("Empty JAR entry name: " + rawName);
        }
        return normalized.toString();
    }

    private static boolean isInvalidatedSignature(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("META-INF/")) {
            return false;
        }
        return upper.endsWith(".SF") || upper.endsWith(".RSA")
                || upper.endsWith(".DSA") || upper.endsWith(".EC")
                || upper.startsWith("META-INF/SIG-");
    }

    private static void writeEntry(JarOutputStream jar, String name, byte[] data) throws IOException {
        JarEntry entry = new JarEntry(name);
        entry.setTime(0L);
        jar.putNextEntry(entry);
        jar.write(data);
        jar.closeEntry();
    }
}
