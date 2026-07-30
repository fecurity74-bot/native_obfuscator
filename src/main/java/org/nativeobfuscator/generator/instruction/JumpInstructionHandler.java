package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;

final class JumpInstructionHandler implements InstructionHandler {
    @Override
    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        int opcode = instruction.getOpcode();
        JumpInsnNode jump = (JumpInsnNode) instruction;
        int target = context.labelId(jump.label);
        if (opcode == Opcodes.GOTO) {
            return "    goto L" + target + ";\n";
        }
        if (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IFLE) {
            return "    if (stack[--sp].i " + unaryOperator(opcode) + " 0) goto L" + target + ";\n";
        }
        if (opcode >= Opcodes.IF_ICMPEQ && opcode <= Opcodes.IF_ICMPLE) {
            int id = context.instructionIndex();
            return "    jint jump_rhs_" + id + " = stack[--sp].i;\n"
                    + "    jint jump_lhs_" + id + " = stack[--sp].i;\n"
                    + "    if (jump_lhs_" + id + " " + integerOperator(opcode)
                    + " jump_rhs_" + id + ") goto L" + target + ";\n";
        }
        if (opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IF_ACMPNE) {
            int id = context.instructionIndex();
            String condition = opcode == Opcodes.IF_ACMPEQ ? "same_" + id : "!same_" + id;
            return "    jobject jump_rhs_" + id + " = stack[--sp].l;\n"
                    + "    jobject jump_lhs_" + id + " = stack[--sp].l;\n"
                    + "    jboolean same_" + id + " = (*env)->IsSameObject(env, jump_lhs_" + id
                    + ", jump_rhs_" + id + ");\n"
                    + "    if (jump_lhs_" + id + " != NULL) (*env)->DeleteLocalRef(env, jump_lhs_" + id + ");\n"
                    + "    if (jump_rhs_" + id + " != NULL) (*env)->DeleteLocalRef(env, jump_rhs_" + id + ");\n"
                    + "    if (" + condition + ") goto L" + target + ";\n";
        }
        if (opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL) {
            int id = context.instructionIndex();
            String operator = opcode == Opcodes.IFNULL ? "==" : "!=";
            return "    jobject jump_ref_" + id + " = stack[--sp].l;\n"
                    + "    jboolean jump_take_" + id + " = jump_ref_" + id + " " + operator + " NULL;\n"
                    + "    if (jump_ref_" + id + " != NULL) (*env)->DeleteLocalRef(env, jump_ref_" + id + ");\n"
                    + "    if (jump_take_" + id + ") goto L" + target + ";\n";
        }
        throw new IllegalArgumentException("Unsupported jump opcode");
    }

    private String unaryOperator(int opcode) {
        return switch (opcode) {
            case Opcodes.IFEQ -> "==";
            case Opcodes.IFNE -> "!=";
            case Opcodes.IFLT -> "<";
            case Opcodes.IFGE -> ">=";
            case Opcodes.IFGT -> ">";
            case Opcodes.IFLE -> "<=";
            default -> throw new IllegalArgumentException();
        };
    }

    private String integerOperator(int opcode) {
        return switch (opcode) {
            case Opcodes.IF_ICMPEQ -> "==";
            case Opcodes.IF_ICMPNE -> "!=";
            case Opcodes.IF_ICMPLT -> "<";
            case Opcodes.IF_ICMPGE -> ">=";
            case Opcodes.IF_ICMPGT -> ">";
            case Opcodes.IF_ICMPLE -> "<=";
            default -> throw new IllegalArgumentException();
        };
    }
}
