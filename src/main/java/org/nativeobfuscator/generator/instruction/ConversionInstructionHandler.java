package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

final class ConversionInstructionHandler implements InstructionHandler {
    @Override
    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        return switch (instruction.getOpcode()) {
            case Opcodes.I2L -> "    stack[sp-1].j = (jlong)stack[sp-1].i;\n";
            case Opcodes.I2F -> "    stack[sp-1].f = (jfloat)stack[sp-1].i;\n";
            case Opcodes.I2D -> "    stack[sp-1].d = (jdouble)stack[sp-1].i;\n";
            case Opcodes.L2I -> "    stack[sp-1].i = (jint)stack[sp-1].j;\n";
            case Opcodes.L2F -> "    stack[sp-1].f = (jfloat)stack[sp-1].j;\n";
            case Opcodes.L2D -> "    stack[sp-1].d = (jdouble)stack[sp-1].j;\n";
            case Opcodes.F2I -> "    stack[sp-1].i = jnic_d2i((jdouble)stack[sp-1].f);\n";
            case Opcodes.F2L -> "    stack[sp-1].j = jnic_d2l((jdouble)stack[sp-1].f);\n";
            case Opcodes.F2D -> "    stack[sp-1].d = (jdouble)stack[sp-1].f;\n";
            case Opcodes.D2I -> "    stack[sp-1].i = jnic_d2i(stack[sp-1].d);\n";
            case Opcodes.D2L -> "    stack[sp-1].j = jnic_d2l(stack[sp-1].d);\n";
            case Opcodes.D2F -> "    stack[sp-1].f = (jfloat)stack[sp-1].d;\n";
            case Opcodes.I2B -> "    stack[sp-1].i = (jbyte)stack[sp-1].i;\n";
            case Opcodes.I2C -> "    stack[sp-1].i = (jchar)stack[sp-1].i;\n";
            case Opcodes.I2S -> "    stack[sp-1].i = (jshort)stack[sp-1].i;\n";
            default -> throw new IllegalArgumentException("Unsupported conversion opcode");
        };
    }
}
