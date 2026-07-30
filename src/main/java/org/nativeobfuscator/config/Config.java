package org.nativeobfuscator.config;

import org.nativeobfuscator.NativeObfuscator;
import lombok.Getter;
import lombok.Setter;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

@Getter
@Setter
public class Config {

    public final File configFile;
    public YamlConfiguration config;

    private String inputJar;
    private String outputJar;
    private List<String> libraries;
    private List<String> include;
    private List<String> exclude;
    private boolean annotationMode;

    private boolean stringEncryption;
    private boolean flowObfuscation;
    private boolean antiDebug;

    public Config() {
        NativeObfuscator.getLogger().info("Loading config...");
        this.configFile = new File("config.yml");
        if (!this.configFile.exists()) {
            this.saveResource("config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(this.configFile);

        this.inputJar = config.getString("input");
        if (this.inputJar == null || this.inputJar.isEmpty()) {
            throw new IllegalArgumentException("Input jar is empty.");
        }
        this.outputJar = config.getString("output");
        if (this.outputJar == null || this.outputJar.isEmpty()) {
            throw new IllegalArgumentException("Output jar is empty.");
        }
        this.libraries = config.getStringList("libs");
        this.include = config.getStringList("includes");
        this.exclude = config.getStringList("excludes");
        this.annotationMode = config.getBoolean("annotationMode", false);

        this.stringEncryption = config.getBoolean("obfuscation.stringEncryption", true);
        this.flowObfuscation = config.getBoolean("obfuscation.flowObfuscation", true);
        this.antiDebug = config.getBoolean("obfuscation.antiDebug", true);
    }

    public void saveResource(@NotNull String resourcePath, boolean replace) {
        if (!resourcePath.isEmpty()) {
            resourcePath = resourcePath.replace('\\', '/');
            InputStream in = this.getResourceAsStream(resourcePath);
            if (in == null) {
                throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
            }
            File outFile = new File(resourcePath);
            int lastIndex = resourcePath.lastIndexOf(47);
            File outDir = new File(resourcePath.substring(0, Math.max(lastIndex, 0)));
            if (!outDir.exists()) {
                outDir.mkdirs();
            }

            try {
                if (outFile.exists() && !replace) return;
                Files.copy(in, outFile.toPath());
            } catch (IOException e) {
                NativeObfuscator.getLogger().error("Could not save " + outFile.getName() + " to " + outFile, e);
            }
        } else {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }
    }

    public InputStream getResourceAsStream(@NotNull String path) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Resource path cannot be null or empty");
        }
        return this.getClass().getClassLoader().getResourceAsStream(path);
    }
}
