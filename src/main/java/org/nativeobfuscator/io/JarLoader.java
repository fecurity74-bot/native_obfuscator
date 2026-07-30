package org.nativeobfuscator.io;

import org.nativeobfuscator.JarArchiveWriter;
import org.nativeobfuscator.NativeObfuscator;
import org.nativeobfuscator.utils.asm.ClassWrapper;
import org.objectweb.asm.ClassReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import java.util.jar.Manifest;

public class JarLoader {

    public void loadInput() {
        NativeObfuscator.getInstance().classes.clear();
        NativeObfuscator.getInstance().classpath.clear();
        NativeObfuscator.getInstance().resources.clear();
        try (JarFile jarFile = new JarFile(NativeObfuscator.getInstance().getConfig().getInputJar())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    manifest.write(baos);
                    NativeObfuscator.getInstance().resources.put("META-INF/MANIFEST.MF", baos.toByteArray());
                }
            }

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String name = JarArchiveWriter.normalizeEntryName(entry.getName());
                if ("META-INF/MANIFEST.MF".equalsIgnoreCase(name)) {
                    continue;
                }
                byte[] content = jarFile.getInputStream(entry).readAllBytes();
                if (name.endsWith(".class")) {
                    String className = name.substring(0, name.length() - 6);
                    
                    try {
                        if (NativeObfuscator.getInstance().classes.containsKey(className)) {
                            NativeObfuscator.getLogger().warn("Ignoring duplicate input class entry: {}", name);
                            continue;
                        }
                        NativeObfuscator.getInstance().classes.put(className, ClassWrapper.from(new ClassReader(content)));
                        NativeObfuscator.getInstance().classpath.put(className, ClassWrapper.fromLib(new ClassReader(content)));
                    } catch (Throwable e) {
                        NativeObfuscator.getLogger().warn(String.format("Error while loading input class: \"%s\" (loading as resources instead)", className));
                        NativeObfuscator.getInstance().resources.putIfAbsent(name, content);
                    }
                } else {
                    if (NativeObfuscator.getInstance().resources.putIfAbsent(name, content) != null) {
                        NativeObfuscator.getLogger().warn("Ignoring duplicate input resource entry: {}", name);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveOutput() {
        String outputPath = NativeObfuscator.getInstance().getConfig().getOutputJar();
        File outputFile = new File(outputPath);
        NativeObfuscator.getLogger().info("Saving output to: " + outputFile.getAbsolutePath());
        
        try {
            JarArchiveWriter.write(outputFile, NativeObfuscator.getInstance().getClasses(),
                    NativeObfuscator.getInstance().getResources());
            NativeObfuscator.getLogger().info("Output saved successfully.");
        } catch (IOException e) {
            NativeObfuscator.getLogger().error("Failed to save output jar", e);
            throw new RuntimeException(e);
        }
    }

    public void loadLib() {
        for (String path : NativeObfuscator.getInstance().getConfig().getLibraries()) {
            File libFile = new File(path);
            if (!libFile.exists()) {
                NativeObfuscator.getLogger().warn(String.format("Lib file \"%s\" not found", path));
                continue;
            }

            if (libFile.isFile()) {
                this.addClasspath(libFile);
            } else if (libFile.isDirectory()) {
                Optional.ofNullable(libFile.listFiles()).ifPresent(files -> {
                    for (File file : files) {
                        this.addClasspath(file);
                    }
                });
            }
        }
    }

    private void addClasspath(File file) {
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    try {
                        NativeObfuscator.getInstance().classpath.put(name.substring(0, name.length() - 6), ClassWrapper.fromLib(new ClassReader(jarFile.getInputStream(entry))));
                    } catch (Throwable e) {
                        NativeObfuscator.getLogger().warn(String.format("Error while loading lib class: \"%s\" (loading as resources instead)", entry.getName()));
                        //this.resources.put(name, IOUtils.toByteArray(jarFile.getInputStream(entry)));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
