package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

final class ArithmeticInstructionHandler implements InstructionHandler {
    @Override
    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        int opcode = instruction.getOpcode();
        return switch (opcode) {
            case Opcodes.IADD -> binary("i", "(jint)((uint32_t)stack[sp-1].i + (uint32_t)stack[sp].i)");
            case Opcodes.LADD -> binary("j", "(jlong)((uint64_t)stack[sp-1].j + (uint64_t)stack[sp].j)");
            case Opcodes.FADD -> binary("f", "stack[sp-1].f + stack[sp].f");
            case Opcodes.DADD -> binary("d", "stack[sp-1].d + stack[sp].d");
            case Opcodes.ISUB -> binary("i", "(jint)((uint32_t)stack[sp-1].i - (uint32_t)stack[sp].i)");
            case Opcodes.LSUB -> binary("j", "(jlong)((uint64_t)stack[sp-1].j - (uint64_t)stack[sp].j)");
            case Opcodes.FSUB -> binary("f", "stack[sp-1].f - stack[sp].f");
            case Opcodes.DSUB -> binary("d", "stack[sp-1].d - stack[sp].d");
            case Opcodes.IMUL -> binary("i", "(jint)((uint32_t)stack[sp-1].i * (uint32_t)stack[sp].i)");
            case Opcodes.LMUL -> binary("j", "(jlong)((uint64_t)stack[sp-1].j * (uint64_t)stack[sp].j)");
            case Opcodes.FMUL -> binary("f", "stack[sp-1].f * stack[sp].f");
            case Opcodes.DMUL -> binary("d", "stack[sp-1].d * stack[sp].d");
            case Opcodes.IDIV -> integralDivision(false, false, context);
            case Opcodes.LDIV -> integralDivision(true, false, context);
            case Opcodes.FDIV -> binary("f", "stack[sp-1].f / stack[sp].f");
            case Opcodes.DDIV -> binary("d", "stack[sp-1].d / stack[sp].d");
            case Opcodes.IREM -> integralDivision(false, true, context);
            case Opcodes.LREM -> integralDivision(true, true, context);
            case Opcodes.FREM -> binary("f", "fmodf(stack[sp-1].f, stack[sp].f)");
            case Opcodes.DREM -> binary("d", "fmod(stack[sp-1].d, stack[sp].d)");
            case Opcodes.INEG -> "    stack[sp-1].i = (jint)(0u - (uint32_t)stack[sp-1].i);\n";
            case Opcodes.LNEG -> "    stack[sp-1].j = (jlong)(0ULL - (uint64_t)stack[sp-1].j);\n";
            case Opcodes.FNEG -> "    stack[sp-1].f = -stack[sp-1].f;\n";
            case Opcodes.DNEG -> "    stack[sp-1].d = -stack[sp-1].d;\n";
            case Opcodes.ISHL -> binary("i",
                    "(jint)((uint32_t)stack[sp-1].i << (((uint32_t)stack[sp].i) & 31u))");
            case Opcodes.LSHL -> binary("j",
                    "(jlong)((uint64_t)stack[sp-1].j << (((uint32_t)stack[sp].i) & 63u))");
            case Opcodes.ISHR -> binary("i", "jnic_ishr(stack[sp-1].i, stack[sp].i)");
            case Opcodes.LSHR -> binary("j", "jnic_lshr(stack[sp-1].j, stack[sp].i)");
            case Opcodes.IUSHR -> binary("i",
                    "(jint)((uint32_t)stack[sp-1].i >> (((uint32_t)stack[sp].i) & 31u))");
            case Opcodes.LUSHR -> binary("j",
                    "(jlong)((uint64_t)stack[sp-1].j >> (((uint32_t)stack[sp].i) & 63u))");
            case Opcodes.IAND -> binary("i", "stack[sp-1].i & stack[sp].i");
            case Opcodes.LAND -> binary("j", "stack[sp-1].j & stack[sp].j");
            case Opcodes.IOR -> binary("i", "stack[sp-1].i | stack[sp].i");
            case Opcodes.LOR -> binary("j", "stack[sp-1].j | stack[sp].j");
            case Opcodes.IXOR -> binary("i", "stack[sp-1].i ^ stack[sp].i");
            case Opcodes.LXOR -> binary("j", "stack[sp-1].j ^ stack[sp].j");
            case Opcodes.LCMP -> compare("j", "0");
            case Opcodes.FCMPL -> compare("f", "-1");
            case Opcodes.FCMPG -> compare("f", "1");
            case Opcodes.DCMPL -> compare("d", "-1");
            case Opcodes.DCMPG -> compare("d", "1");
            default -> throw new IllegalArgumentException("Unsupported arithmetic opcode");
        };
    }

    private String binary(String field, String expression) {
        return "    sp--; stack[sp-1]." + field + " = " + expression + ";\n";
    }

    private String integralDivision(boolean wide, boolean remainder, InstructionContext context) {
        String field = wide ? "j" : "i";
        String min = wide ? "INT64_MIN" : "INT32_MIN";
        String operation = remainder ? "%" : "/";
        String overflowResult = remainder ? "0" : min;
        return "    sp--;\n"
                + "    if (stack[sp]." + field + " == 0) {\n"
                + "        throw_arith(env, \"/ by zero\");\n"
                + context.exceptionHandler().apply(context.instructionIndex())
                + "    } else {\n"
                + "        stack[sp-1]." + field + " = (stack[sp-1]." + field + " == " + min
                + " && stack[sp]." + field + " == -1) ? " + overflowResult
                + " : stack[sp-1]." + field + " " + operation + " stack[sp]." + field + ";\n"
                + "    }\n";
    }

    private String compare(String field, String unorderedResult) {
        return "    sp--;\n"
                + "    if (stack[sp-1]." + field + " > stack[sp]." + field + ") stack[sp-1].i = 1;\n"
                + "    else if (stack[sp-1]." + field + " == stack[sp]." + field + ") stack[sp-1].i = 0;\n"
                + "    else if (stack[sp-1]." + field + " < stack[sp]." + field + ") stack[sp-1].i = -1;\n"
                + "    else stack[sp-1].i = " + unorderedResult + ";\n";
    }
}
