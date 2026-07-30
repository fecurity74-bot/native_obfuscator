package org.nativeobfuscator;

import org.nativeobfuscator.utils.asm.ClassWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JarArchiveWriterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void normalizesWindowsSeparators() throws Exception {
        assertEquals("a/b/C.class", JarArchiveWriter.normalizeEntryName("\\a\\b\\C.class"));
    }

    @Test
    void rejectsParentTraversal() {
        assertThrows(IOException.class,
                () -> JarArchiveWriter.normalizeEntryName("a/../secret.class"));
    }

    @Test
    void writesEveryNormalizedNameOnceAndDropsStaleSignatures() throws Exception {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "a/B", null, "java/lang/Object", null);
        writer.visitEnd();
        ClassWrapper clazz = ClassWrapper.from(new ClassReader(writer.toByteArray()));
        Path output = temporaryDirectory.resolve("nested/output.jar");

        JarArchiveWriter.write(output.toFile(), Map.of("a/B", clazz), Map.of(
                "a\\B.class", new byte[] { 1, 2, 3 },
                "META-INF/OLD.SF", new byte[] { 4 },
                "assets\\value.txt", new byte[] { 5 }));

        try (JarFile jar = new JarFile(output.toFile())) {
            List<String> names = jar.stream().map(entry -> entry.getName()).toList();
            assertEquals(1, names.stream().filter("a/B.class"::equals).count());
            assertTrue(names.contains("assets/value.txt"));
            assertFalse(names.contains("META-INF/OLD.SF"));
            assertTrue(jar.getInputStream(jar.getJarEntry("a/B.class")).readAllBytes().length > 3);
        }
    }
}
