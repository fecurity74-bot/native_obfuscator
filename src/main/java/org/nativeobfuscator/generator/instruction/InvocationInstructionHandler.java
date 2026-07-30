package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Opcodes;

/**
 * Selects the JNI dispatch primitive that preserves the corresponding JVM
 * invocation semantics.
 */
public final class InvocationInstructionHandler {
    private InvocationInstructionHandler() {
    }

    public static String callFunction(int opcode, String returnType) {
        return switch (opcode) {
            case Opcodes.INVOKESTATIC -> "CallStatic" + returnType + "MethodA";
            case Opcodes.INVOKESPECIAL -> "CallNonvirtual" + returnType + "MethodA";
            case Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEINTERFACE ->
                    "Call" + returnType + "MethodA";
            default -> throw new IllegalArgumentException("Not a method invocation opcode: " + opcode);
        };
    }

    public static boolean requiresOwnerClassArgument(int opcode) {
        return opcode == Opcodes.INVOKESPECIAL;
    }
}
