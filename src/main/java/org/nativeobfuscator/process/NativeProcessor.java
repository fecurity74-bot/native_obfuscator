package org.nativeobfuscator.process;

import org.nativeobfuscator.NativeObfuscator;
import org.nativeobfuscator.generator.Generator;
import org.nativeobfuscator.generator.instruction.OpcodeSupport;
import org.nativeobfuscator.process.compiler.ZigCompiler;
import org.nativeobfuscator.utils.MatcherUtils;
import org.nativeobfuscator.utils.asm.ClassWrapper;
import org.nativeobfuscator.utils.asm.MethodWrapper;
import lombok.Getter;
import org.objectweb.asm.Opcodes;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import org.objectweb.asm.ClassReader;

public class NativeProcessor {
    private static final String INCLUDE_ANNOTATION =
            "Lorg/nativeobfuscator/annotation/Include;";
    @Getter
    private final NativeObfuscator nativeObfuscator;
    private final Generator generator;
    private final Set<String> generatedNativeMethods = new HashSet<>();
    private final Set<ClassWrapper> processedClasses = new HashSet<>();

    public NativeProcessor(NativeObfuscator nativeObfuscator) {
        this.nativeObfuscator = nativeObfuscator;
        this.generator = new Generator(this);
    }

    public boolean isNative(String owner, String name, String desc) {
        if (generatedNativeMethods.contains(methodKey(owner, name, desc))) {
            return true;
        }
        ClassWrapper classWrapper = nativeObfuscator.getClasses().get(owner);
        if (classWrapper == null)
            return false;
        if (!shouldProcessClass(classWrapper))
            return false;

        for (MethodWrapper mw : classWrapper.getMethods()) {
            if (mw.getOriginalName().equals(name) && mw.getOriginalDescriptor().equals(desc)) {
                return shouldProcessMethod(mw);
            }
        }
        return false;
    }

    public void process() {
        NativeObfuscator.getLogger().info("Starting native processing...");

        HashMap<String, ClassWrapper> temp = new HashMap<>();
        for (ClassWrapper classWrapper : nativeObfuscator.getClasses().values()) {
            if (!shouldProcessClass(classWrapper))
                continue;

            boolean classModified = false;
            // Iterate over a copy to avoid ConcurrentModificationException when adding
            // helper methods
            List<MethodWrapper> methods = new ArrayList<>(classWrapper.getMethods());
            for (MethodWrapper methodWrapper : methods) {
                if (shouldProcessMethod(methodWrapper)) {
                    processMethod(classWrapper, methodWrapper);
                    classModified = true;
                }
            }

            if (classModified) {
                processedClasses.add(classWrapper);
                injectLoader(classWrapper, temp);
            }
        }

        nativeObfuscator.getClasses().putAll(temp);

        // Finalize generation (write C files, compile, etc.)
        generator.finalizeGeneration();

        // Extract jni.h from resources
        try (InputStream is = getClass().getResourceAsStream("/jni.h")) {
            if (is != null) {
                Files.copy(is, new File(nativeObfuscator.getTmpdir(), "jni.h").toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                NativeObfuscator.getLogger().warn("jni.h not found in resources. Compilation might fail if system headers are missing.");
            }
        } catch (IOException e) {
            NativeObfuscator.getLogger().error("Failed to extract jni.h: " + e.getMessage());
        }

        // Compile using Zig
        File cFile = new File(nativeObfuscator.getTmpdir(), "native-lib.c");

        if (cFile.exists()) {
            if (!ZigCompiler.compile(cFile, nativeObfuscator.getTmpdir())) {
                throw new IllegalStateException("Native compilation failed; output JAR was not written. "
                        + "Generated sources are in " + nativeObfuscator.getTmpdir().getAbsolutePath());
            }
            File library = new File(nativeObfuscator.getTmpdir(), "j2c.dll");
            if (!library.isFile() || library.length() == 0) {
                throw new IllegalStateException("Native compilation produced no j2c.dll");
            }
            try {
                nativeObfuscator.getResources().put("j2c.dll", Files.readAllBytes(library.toPath()));
                NativeObfuscator.getLogger().info("Added j2c.dll to output JAR.");
            } catch (IOException error) {
                throw new UncheckedIOException("Failed to read compiled j2c.dll", error);
            }
        } else {
            throw new IllegalStateException("Native source file not found: " + cFile.getAbsolutePath());
        }
    }

    private boolean shouldProcessClass(ClassWrapper classWrapper) {
        String className = classWrapper.getName();

        // 1. Check excludes first
        List<String> excludes = nativeObfuscator.getConfig().getExclude();
        if (excludes != null) {
            for (String exclude : excludes) {
                if (MatcherUtils.match(className, exclude)) {
                    return false;
                }
            }
        }

        // 2. Check includes
        List<String> includes = nativeObfuscator.getConfig().getInclude();
        if (includes != null && !includes.isEmpty()) {
            boolean included = false;
            for (String include : includes) {
                if (MatcherUtils.match(className, include)) {
                    included = true;
                    break;
                }
            }
            if (!included)
                return false;
        }

        // Basic sanity checks
        return (classWrapper.getClassNode().access & Opcodes.ACC_INTERFACE) == 0;
    }

    private boolean shouldProcessMethod(MethodWrapper methodWrapper) {
        String name = methodWrapper.getOriginalName();
        // Skip constructors and static initializers
        if ("<init>".equals(name) || "<clinit>".equals(name)) {
            return false;
        }

        String desc = methodWrapper.getOriginalDescriptor();
        if ("findClass".equals(name) && "(Ljava/lang/String;)Ljava/lang/Class;".equals(desc)) {
            return false;
        }
        if ("getResourceAsStream".equals(name) && "(Ljava/lang/String;)Ljava/io/InputStream;".equals(desc)) {
            return false;
        }
        if (nativeObfuscator.getConfig().isAnnotationMode()
                && !hasIncludeAnnotation(methodWrapper.getMethodNode())) {
            return false;
        }

        if (hasUnsupportedOpcodes(methodWrapper.getMethodNode())) {
            NativeObfuscator.getLogger().warn("Skipping method with unsupported opcodes: " + name);
            return false;
        }

        return (methodWrapper.getMethodNode().access & Opcodes.ACC_ABSTRACT) == 0 &&
                (methodWrapper.getMethodNode().access & Opcodes.ACC_NATIVE) == 0 &&
                methodWrapper.getMethodNode().instructions.size() > 0;
    }

    private boolean hasIncludeAnnotation(MethodNode method) {
        return containsAnnotation(method.visibleAnnotations)
                || containsAnnotation(method.invisibleAnnotations);
    }

    private boolean containsAnnotation(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return false;
        }
        for (AnnotationNode annotation : annotations) {
            if (INCLUDE_ANNOTATION.equals(annotation.desc)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUnsupportedOpcodes(MethodNode methodNode) {
        for (AbstractInsnNode insn = methodNode.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            int opcode = insn.getOpcode();
            if (opcode < 0)
                continue;
            if (!OpcodeSupport.isSupported(opcode)) {
                return true;
            }
        }
        return false;
    }

    private void injectLoader(ClassWrapper classWrapper, HashMap<String, ClassWrapper> classes) {
        try {
            String loader = "org/nativeobfuscator/NativeLoader";
            if (!nativeObfuscator.getClasses().containsKey(loader) && !classes.containsKey(loader)) {
                InputStream is = getClass().getResourceAsStream("/" + loader + ".class");
                if (is == null) {
                    throw new IOException("Could not find NativeLoader.class to inject!");
                }
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    try (DataInputStream dis = new DataInputStream(is)) {
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = dis.read(buffer)) != -1) {
                            baos.write(buffer, 0, read);
                        }
                    }
                    classes.put(loader, ClassWrapper.from(new ClassReader(baos.toByteArray())));
                }
            }
        } catch (Exception e) {
            NativeObfuscator.getLogger().error("Failed to inject NativeLoader", e);
        }

        InsnList il = new InsnList();
        il.add(new LdcInsnNode(Type.getObjectType(classWrapper.getName()))); // Push class
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/nativeobfuscator/NativeLoader", "load",
                "(Ljava/lang/Class;)V", false));

        MethodNode clinit = classWrapper.getMethodNode("<clinit>", "()V");
        if (clinit == null) {
            clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            classWrapper.addMethod(clinit);
            clinit.instructions.add(il);
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
        } else {
            // Loading at the beginning preserves all existing control-flow exits and
            // exception-table boundaries. Removing the last node used to corrupt
            // <clinit> methods whose final node was a label/frame rather than RETURN.
            clinit.instructions.insert(il);
        }
    }

    private void processMethod(ClassWrapper owner, MethodWrapper method) {
        NativeObfuscator.getLogger().info("Processing method: " + owner.getName() + "." + method.getOriginalName());

        // Handle INVOKEDYNAMIC before generation
        handleInvokeDynamic(owner, method);

        // Generate native code before replacing the Java implementation.
        generator.generateMethod(owner, method);

        // 2. Modify Java method to be native
        method.getMethodNode().access |= Opcodes.ACC_NATIVE;
        method.getMethodNode().instructions.clear();
        method.getMethodNode().tryCatchBlocks.clear();
        method.getMethodNode().localVariables.clear();

        generatedNativeMethods.add(methodKey(owner.getName(), method.getOriginalName(),
                method.getOriginalDescriptor()));
    }

    private void handleInvokeDynamic(ClassWrapper owner, MethodWrapper method) {
        InsnList instructions = method.getMethodNode().instructions;
        List<InvokeDynamicInsnNode> indyNodes = new ArrayList<>();

        for (AbstractInsnNode insn = instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof InvokeDynamicInsnNode) {
                indyNodes.add((InvokeDynamicInsnNode) insn);
            }
        }

        for (InvokeDynamicInsnNode indy : indyNodes) {
            String helperName = "lamda$" + Math.abs(indy.hashCode());

            // Create helper method: static synthetic
            MethodNode helper = new MethodNode(Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, helperName, indy.desc, null,
                    null);

            // Generate body
            InsnList il = helper.instructions;
            Type[] args = Type.getArgumentTypes(indy.desc);
            int varIndex = 0;
            for (Type arg : args) {
                il.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), varIndex));
                varIndex += arg.getSize();
            }

            // Add the invokedynamic instruction (clone it to be safe)
            il.add(indy.clone(null));

            // Return
            Type returnType = Type.getReturnType(indy.desc);
            il.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));

            // Add helper to class
            owner.addMethod(helper);

            // Replace original instruction with INVOKESTATIC to helper
            instructions.set(indy,
                    new MethodInsnNode(Opcodes.INVOKESTATIC, owner.getName(), helperName, indy.desc, false));
        }
    }

    private static String methodKey(String owner, String name, String descriptor) {
        return owner + '\0' + name + '\0' + descriptor;
    }
}
