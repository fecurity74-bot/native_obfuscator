package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;

final class ConstantInstructionHandler implements InstructionHandler {
    @Override
    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        int opcode = instruction.getOpcode();
        return switch (opcode) {
            case Opcodes.NOP -> "";
            case Opcodes.ACONST_NULL -> "    stack[sp++].l = NULL;\n";
            case Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1,
                    Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4,
                    Opcodes.ICONST_5 ->
                    "    stack[sp++].i = " + (opcode - Opcodes.ICONST_0) + ";\n";
            case Opcodes.LCONST_0, Opcodes.LCONST_1 ->
                    "    stack[sp++].j = " + (opcode - Opcodes.LCONST_0) + "LL;\n";
            case Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2 ->
                    "    stack[sp++].f = " + (opcode - Opcodes.FCONST_0) + ".0f;\n";
            case Opcodes.DCONST_0, Opcodes.DCONST_1 ->
                    "    stack[sp++].d = " + (opcode - Opcodes.DCONST_0) + ".0;\n";
            case Opcodes.BIPUSH, Opcodes.SIPUSH ->
                    "    stack[sp++].i = " + ((IntInsnNode) instruction).operand + ";\n";
            default -> throw new IllegalArgumentException("Unsupported constant opcode");
        };
    }
}
