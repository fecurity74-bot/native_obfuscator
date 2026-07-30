package org.nativeobfuscator;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class NativeLoader {
    private static boolean loaded;

    private NativeLoader() {
    }

    public static synchronized void load(Class<?> targetClass) {
        if (!loaded) {
            try (InputStream library = NativeLoader.class.getResourceAsStream("/j2c.dll")) {
                if (library == null) {
                    throw new UnsatisfiedLinkError("j2c.dll is missing from the application JAR");
                }
                File extracted = Files.createTempFile("j2c-", ".dll").toFile();
                Files.copy(library, extracted.toPath(), StandardCopyOption.REPLACE_EXISTING);
                extracted.deleteOnExit();
                System.load(extracted.getAbsolutePath());
                loaded = true;
            } catch (UnsatisfiedLinkError error) {
                throw error;
            } catch (Exception error) {
                throw new RuntimeException("Failed to load j2c.dll", error);
            }
        }
        registerNatives(targetClass);
    }

    private static native void registerNatives(Class<?> targetClass);
}
