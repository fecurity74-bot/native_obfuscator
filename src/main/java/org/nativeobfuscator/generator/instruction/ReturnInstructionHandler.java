package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

final class ReturnInstructionHandler implements InstructionHandler {
    @Override
    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        return switch (instruction.getOpcode()) {
            case Opcodes.RETURN -> "    (*env)->PopLocalFrame(env, NULL);\n    return;\n";
            case Opcodes.IRETURN -> "    (*env)->PopLocalFrame(env, NULL);\n    return stack[--sp].i;\n";
            case Opcodes.LRETURN -> "    (*env)->PopLocalFrame(env, NULL);\n    return stack[--sp].j;\n";
            case Opcodes.FRETURN -> "    (*env)->PopLocalFrame(env, NULL);\n    return stack[--sp].f;\n";
            case Opcodes.DRETURN -> "    (*env)->PopLocalFrame(env, NULL);\n    return stack[--sp].d;\n";
            case Opcodes.ARETURN -> "    return (*env)->PopLocalFrame(env, stack[--sp].l);\n";
            default -> throw new IllegalArgumentException("Unsupported return opcode");
        };
    }
}
