package org.nativeobfuscator;

import org.nativeobfuscator.config.Config;
import org.nativeobfuscator.io.JarLoader;
import org.nativeobfuscator.process.NativeProcessor;
import org.nativeobfuscator.utils.asm.ClassWrapper;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class NativeObfuscator {

    @Getter
    public static Logger logger = LogManager.getLogger(NativeObfuscator.class);
    private final JarLoader loader;
    private final NativeProcessor processor;

    public final Config config;

    public final Map<String, ClassWrapper> classes;
    public final Map<String, ClassWrapper> classpath;
    public final Map<String, byte[]> resources;

    public final File tmpdir = new File(System.getProperty("java.io.tmpdir"), "j2c_" + UUID.randomUUID());

    public NativeObfuscator() {
        this.config = new Config();
        this.classes = new HashMap<>();
        this.classpath = new HashMap<>();
        this.resources = new HashMap<>();
        this.loader = new JarLoader(this);
        this.processor = new NativeProcessor(this);
    }

    public void run() {
        if (!tmpdir.exists() && !tmpdir.mkdirs()) {
            throw new IllegalStateException("Unable to create temporary directory: " + tmpdir);
        }
        this.loader.loadInput();
        this.loader.loadLib();
        this.processor.process();
        this.loader.saveOutput();
        this.deleteDirectory(this.tmpdir);
    }

    private void cleanup() {
        logger.info("Skipping cleanup for debug...");
        deleteDirectory(this.tmpdir);
        /*File zigCache = new File(this.configFile.getParentFile(), ".zig-cache");
        deleteDirectory(zigCache);*/
    }

    private void deleteFile(File file) {
        if (!file.exists()) return;
        if (!file.delete()) {
            logger.warn("无法删除文件: " + file.getAbsolutePath());
        }
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        deleteFile(file);
                    }
                }
            }
            deleteFile(directory);
        }
    }
}
