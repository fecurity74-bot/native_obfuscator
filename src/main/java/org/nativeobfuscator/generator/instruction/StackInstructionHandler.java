package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

/**
 * Implements the category-1/category-2 forms of DUP2_X1 and DUP2_X2 using
 * the generator's compressed (one C slot per JVM value) stack.
 */
final class StackInstructionHandler implements InstructionHandler {
    @Override
    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        return switch (instruction.getOpcode()) {
            case Opcodes.POP -> pop(context);
            case Opcodes.POP2 -> pop2(context);
            case Opcodes.DUP -> dup(context);
            case Opcodes.DUP_X1 -> dupX1(context);
            case Opcodes.DUP_X2 -> dupX2(context);
            case Opcodes.DUP2 -> dup2(context);
            case Opcodes.DUP2_X1 -> dup2X1(context);
            case Opcodes.DUP2_X2 -> dup2X2(context);
            case Opcodes.SWAP -> "    StackValue swap_tmp = stack[sp-1];\n"
                    + "    stack[sp-1] = stack[sp-2];\n"
                    + "    stack[sp-2] = swap_tmp;\n";
            default -> throw new IllegalArgumentException("Unsupported stack opcode");
        };
    }

    private String pop(InstructionContext context) {
        BasicValue value = top(context.frame(), 0);
        return deleteReference("stack[sp-1]", value, context) + "    sp--;\n";
    }

    private String pop2(InstructionContext context) {
        BasicValue value1 = top(context.frame(), 0);
        if (value1.getSize() == 2) {
            return "    sp--;\n";
        }
        BasicValue value2 = top(context.frame(), 1);
        if (value2.getSize() != 1) {
            throw new IllegalArgumentException("Invalid POP2 operand categories");
        }
        return deleteReference("stack[sp-1]", value1, context)
                + deleteReference("stack[sp-2]", value2, context)
                + "    sp -= 2;\n";
    }

    private String dup(InstructionContext context) {
        BasicValue value = top(context.frame(), 0);
        StringBuilder code = new StringBuilder();
        duplicate(code, "stack[sp]", "stack[sp-1]", value, context);
        code.append("    sp++;\n");
        return code.toString();
    }

    private String dupX1(InstructionContext context) {
        BasicValue value1 = top(context.frame(), 0);
        if (value1.getSize() != 1 || top(context.frame(), 1).getSize() != 1) {
            throw new IllegalArgumentException("Invalid DUP_X1 operand categories");
        }
        StringBuilder code = new StringBuilder();
        code.append("    StackValue dx1_v1 = stack[sp-1];\n");
        code.append("    stack[sp-1] = stack[sp-2];\n");
        code.append("    stack[sp-2] = dx1_v1;\n");
        duplicate(code, "stack[sp]", "dx1_v1", value1, context);
        code.append("    sp++;\n");
        return code.toString();
    }

    private String dupX2(InstructionContext context) {
        BasicValue value1 = top(context.frame(), 0);
        BasicValue value2 = top(context.frame(), 1);
        if (value1.getSize() != 1) {
            throw new IllegalArgumentException("Invalid DUP_X2 operand categories");
        }
        StringBuilder code = new StringBuilder();
        code.append("    StackValue dx2_v1 = stack[sp-1];\n");
        if (value2.getSize() == 2) {
            code.append("    stack[sp-1] = stack[sp-2];\n");
            code.append("    stack[sp-2] = dx2_v1;\n");
        } else {
            if (top(context.frame(), 2).getSize() != 1) {
                throw new IllegalArgumentException("Invalid DUP_X2 operand categories");
            }
            code.append("    stack[sp-1] = stack[sp-2];\n");
            code.append("    stack[sp-2] = stack[sp-3];\n");
            code.append("    stack[sp-3] = dx2_v1;\n");
        }
        duplicate(code, "stack[sp]", "dx2_v1", value1, context);
        code.append("    sp++;\n");
        return code.toString();
    }

    private String dup2(InstructionContext context) {
        BasicValue value1 = top(context.frame(), 0);
        StringBuilder code = new StringBuilder();
        if (value1.getSize() == 2) {
            duplicate(code, "stack[sp]", "stack[sp-1]", value1, context);
            code.append("    sp++;\n");
            return code.toString();
        }
        BasicValue value2 = top(context.frame(), 1);
        if (value2.getSize() != 1) {
            throw new IllegalArgumentException("Invalid DUP2 operand categories");
        }
        duplicate(code, "stack[sp]", "stack[sp-2]", value2, context);
        duplicate(code, "stack[sp+1]", "stack[sp-1]", value1, context);
        code.append("    sp += 2;\n");
        return code.toString();
    }

    private String dup2X1(InstructionContext context) {
        Frame<BasicValue> frame = context.frame();
        BasicValue value1 = top(frame, 0);
        StringBuilder code = new StringBuilder();
        if (value1.getSize() == 2) {
            // ..., value2(cat1), value1(cat2) -> ..., value1, value2, value1
            code.append("    StackValue d21_v1 = stack[sp-1];\n");
            code.append("    StackValue d21_v2 = stack[sp-2];\n");
            code.append("    stack[sp-2] = d21_v1;\n");
            code.append("    stack[sp-1] = d21_v2;\n");
            code.append("    stack[sp] = d21_v1;\n");
            code.append("    sp++;\n");
        } else {
            BasicValue value2 = top(frame, 1);
            if (value2.getSize() != 1 || top(frame, 2).getSize() != 1) {
                throw new IllegalArgumentException("Invalid DUP2_X1 operand categories");
            }
            // ..., value3, value2, value1 -> ..., value2, value1, value3, value2, value1
            code.append("    StackValue d21_v1 = stack[sp-1];\n");
            code.append("    StackValue d21_v2 = stack[sp-2];\n");
            code.append("    StackValue d21_v3 = stack[sp-3];\n");
            code.append("    stack[sp-3] = d21_v2;\n");
            code.append("    stack[sp-2] = d21_v1;\n");
            code.append("    stack[sp-1] = d21_v3;\n");
            duplicate(code, "stack[sp]", "d21_v2", value2, context);
            duplicate(code, "stack[sp+1]", "d21_v1", value1, context);
            code.append("    sp += 2;\n");
        }
        return code.toString();
    }

    private String dup2X2(InstructionContext context) {
        Frame<BasicValue> frame = context.frame();
        BasicValue value1 = top(frame, 0);
        StringBuilder code = new StringBuilder();
        if (value1.getSize() == 2) {
            BasicValue value2 = top(frame, 1);
            if (value2.getSize() == 2) {
                // ..., value2(cat2), value1(cat2) -> ..., value1, value2, value1
                code.append("    StackValue d22_v1 = stack[sp-1];\n");
                code.append("    StackValue d22_v2 = stack[sp-2];\n");
                code.append("    stack[sp-2] = d22_v1;\n");
                code.append("    stack[sp-1] = d22_v2;\n");
                code.append("    stack[sp] = d22_v1;\n");
                code.append("    sp++;\n");
            } else {
                BasicValue value3 = top(frame, 2);
                if (value2.getSize() != 1 || value3.getSize() != 1) {
                    throw new IllegalArgumentException("Invalid DUP2_X2 operand categories");
                }
                // ..., value3, value2, value1(cat2) -> ..., value1, value3, value2, value1
                code.append("    StackValue d22_v1 = stack[sp-1];\n");
                code.append("    StackValue d22_v2 = stack[sp-2];\n");
                code.append("    StackValue d22_v3 = stack[sp-3];\n");
                code.append("    stack[sp-3] = d22_v1;\n");
                code.append("    stack[sp-2] = d22_v3;\n");
                code.append("    stack[sp-1] = d22_v2;\n");
                code.append("    stack[sp] = d22_v1;\n");
                code.append("    sp++;\n");
            }
        } else {
            BasicValue value2 = top(frame, 1);
            if (value2.getSize() != 1) {
                throw new IllegalArgumentException("Invalid DUP2_X2 operand categories");
            }
            BasicValue value3 = top(frame, 2);
            if (value3.getSize() == 2) {
                // ..., value3(cat2), value2, value1 -> ..., value2, value1, value3, value2, value1
                code.append("    StackValue d22_v1 = stack[sp-1];\n");
                code.append("    StackValue d22_v2 = stack[sp-2];\n");
                code.append("    StackValue d22_v3 = stack[sp-3];\n");
                code.append("    stack[sp-3] = d22_v2;\n");
                code.append("    stack[sp-2] = d22_v1;\n");
                code.append("    stack[sp-1] = d22_v3;\n");
                duplicate(code, "stack[sp]", "d22_v2", value2, context);
                duplicate(code, "stack[sp+1]", "d22_v1", value1, context);
                code.append("    sp += 2;\n");
            } else {
                BasicValue value4 = top(frame, 3);
                if (value3.getSize() != 1 || value4.getSize() != 1) {
                    throw new IllegalArgumentException("Invalid DUP2_X2 operand categories");
                }
                // ..., value4, value3, value2, value1 -> ..., value2, value1, value4, value3, value2, value1
                code.append("    StackValue d22_v1 = stack[sp-1];\n");
                code.append("    StackValue d22_v2 = stack[sp-2];\n");
                code.append("    StackValue d22_v3 = stack[sp-3];\n");
                code.append("    StackValue d22_v4 = stack[sp-4];\n");
                code.append("    stack[sp-4] = d22_v2;\n");
                code.append("    stack[sp-3] = d22_v1;\n");
                code.append("    stack[sp-2] = d22_v4;\n");
                code.append("    stack[sp-1] = d22_v3;\n");
                duplicate(code, "stack[sp]", "d22_v2", value2, context);
                duplicate(code, "stack[sp+1]", "d22_v1", value1, context);
                code.append("    sp += 2;\n");
            }
        }
        return code.toString();
    }

    private void duplicate(StringBuilder code, String target, String source,
            BasicValue value, InstructionContext context) {
        if (context.isReference(value)) {
            code.append("    ").append(target).append(".l = (*env)->NewLocalRef(env, ")
                    .append(source).append(".l);\n");
        } else {
            code.append("    ").append(target).append(" = ").append(source).append(";\n");
        }
    }

    private String deleteReference(String slot, BasicValue value, InstructionContext context) {
        if (!context.isReference(value)) {
            return "";
        }
        return "    if (" + slot + ".l != NULL) (*env)->DeleteLocalRef(env, " + slot + ".l);\n";
    }

    private BasicValue top(Frame<BasicValue> frame, int offset) {
        int index = frame.getStackSize() - 1 - offset;
        if (index < 0) {
            throw new IllegalArgumentException("Operand stack underflow");
        }
        return frame.getStack(index);
    }
}
