package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

final class LocalInstructionHandler implements InstructionHandler {
    @Override
    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        int opcode = instruction.getOpcode();
        if (opcode == Opcodes.IINC) {
            IincInsnNode increment = (IincInsnNode) instruction;
            return "    locals[" + increment.var + "].i = (jint)((uint32_t)locals["
                    + increment.var + "].i + (uint32_t)(" + increment.incr + "));\n";
        }

        int variable = ((VarInsnNode) instruction).var;
        return switch (opcode) {
            case Opcodes.ILOAD -> "    stack[sp++].i = locals[" + variable + "].i;\n";
            case Opcodes.LLOAD -> "    stack[sp++].j = locals[" + variable + "].j;\n";
            case Opcodes.FLOAD -> "    stack[sp++].f = locals[" + variable + "].f;\n";
            case Opcodes.DLOAD -> "    stack[sp++].d = locals[" + variable + "].d;\n";
            case Opcodes.ALOAD -> "    stack[sp++].l = (*env)->NewLocalRef(env, locals["
                    + variable + "].l);\n";
            case Opcodes.ISTORE -> "    locals[" + variable + "].i = stack[--sp].i;\n";
            case Opcodes.LSTORE -> "    locals[" + variable + "].j = stack[--sp].j;\n";
            case Opcodes.FSTORE -> "    locals[" + variable + "].f = stack[--sp].f;\n";
            case Opcodes.DSTORE -> "    locals[" + variable + "].d = stack[--sp].d;\n";
            case Opcodes.ASTORE -> referenceStore(variable, context.instructionIndex());
            default -> throw new IllegalArgumentException("Unsupported local opcode");
        };
    }

    private String referenceStore(int variable, int id) {
        return "    {\n"
                + "        jobject astore_" + id + " = stack[--sp].l;\n"
                + "        if (locals[" + variable + "].l != NULL && locals[" + variable
                + "].l != astore_" + id + ") {\n"
                + "            (*env)->DeleteLocalRef(env, locals[" + variable + "].l);\n"
                + "        }\n"
                + "        locals[" + variable + "].l = astore_" + id + ";\n"
                + "    }\n";
    }
}
