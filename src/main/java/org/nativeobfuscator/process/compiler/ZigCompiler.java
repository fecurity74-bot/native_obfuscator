package org.nativeobfuscator.process.compiler;

import org.nativeobfuscator.NativeObfuscator;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class ZigCompiler {
    private static final String WINDOWS_TARGET = "x86_64-windows";

    private ZigCompiler() {
    }

    public static boolean compile(File cFile, File outputDirectory) {
        File zig = findZigExecutable();
        String executable = zig == null ? "zig" : zig.getAbsolutePath();
        File output = new File(outputDirectory, "j2c.dll");

        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("cc");
        command.add("-target");
        command.add(WINDOWS_TARGET);
        command.add("-shared");
        command.add("-O3");
        command.add("-o");
        command.add(output.getAbsolutePath());
        command.add(cFile.getAbsolutePath());

        NativeObfuscator.getLogger().info("Compiling j2c.dll for Windows x64...");
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            StringBuilder compilerOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    compilerOutput.append(line).append(System.lineSeparator());
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                NativeObfuscator.getLogger().error("Zig failed with exit code {}:\n{}",
                        exitCode, compilerOutput);
                return false;
            }
            if (!output.isFile() || output.length() == 0) {
                NativeObfuscator.getLogger().error("Zig returned success but j2c.dll was not created.");
                return false;
            }
            NativeObfuscator.getLogger().info("Compilation successful: j2c.dll");
            return true;
        } catch (Exception error) {
            NativeObfuscator.getLogger().error("Failed to compile j2c.dll", error);
            return false;
        }
    }

    private static File findZigExecutable() {
        File[] candidates = {
                new File("zig.exe"),
                new File("zig/zig.exe"),
                new File("build/libs/zig.exe"),
                new File("../zig.exe")
        };
        for (File candidate : candidates) {
            if (candidate.isFile()) {
                return candidate;
            }
        }

        File currentDirectory = new File(".");
        File[] children = currentDirectory.listFiles(File::isDirectory);
        if (children != null) {
            for (File child : children) {
                File direct = new File(child, "zig.exe");
                if (direct.isFile()) {
                    return direct;
                }
                File[] nested = child.listFiles(File::isDirectory);
                if (nested != null) {
                    for (File directory : nested) {
                        File executable = new File(directory, "zig.exe");
                        if (executable.isFile()) {
                            return executable;
                        }
                    }
                }
            }
        }
        return null;
    }
}
