package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

final class ArrayCreationInstructionHandler implements InstructionHandler {
    @Override
    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        return switch (instruction.getOpcode()) {
            case Opcodes.NEWARRAY -> primitiveArray((IntInsnNode) instruction, context);
            case Opcodes.ANEWARRAY -> referenceArray((TypeInsnNode) instruction, context);
            case Opcodes.ARRAYLENGTH -> arrayLength(context);
            default -> throw new IllegalArgumentException("Unsupported array opcode");
        };
    }

    private String primitiveArray(IntInsnNode instruction, InstructionContext context) {
        String function = switch (instruction.operand) {
            case Opcodes.T_BOOLEAN -> "NewBooleanArray";
            case Opcodes.T_CHAR -> "NewCharArray";
            case Opcodes.T_FLOAT -> "NewFloatArray";
            case Opcodes.T_DOUBLE -> "NewDoubleArray";
            case Opcodes.T_BYTE -> "NewByteArray";
            case Opcodes.T_SHORT -> "NewShortArray";
            case Opcodes.T_INT -> "NewIntArray";
            case Opcodes.T_LONG -> "NewLongArray";
            default -> throw new IllegalArgumentException(
                    "Unsupported NEWARRAY type " + instruction.operand);
        };
        return "    jint array_length = stack[--sp].i;\n"
                + "    stack[sp++].l = (*env)->" + function + "(env, array_length);\n"
                + context.exceptionHandler().apply(context.instructionIndex());
    }

    private String referenceArray(TypeInsnNode instruction, InstructionContext context) {
        String id = Integer.toUnsignedString(context.instructionIndex());
        return "    jint array_length_" + id + " = stack[--sp].i;\n"
                + "    jclass component_" + id + " = (*env)->FindClass(env, \""
                + instruction.desc + "\");\n"
                + "    stack[sp++].l = component_" + id + " == NULL ? NULL\n"
                + "        : (*env)->NewObjectArray(env, array_length_" + id
                + ", component_" + id + ", NULL);\n"
                + "    if (component_" + id + " != NULL) "
                + "(*env)->DeleteLocalRef(env, component_" + id + ");\n"
                + context.exceptionHandler().apply(context.instructionIndex());
    }

    private String arrayLength(InstructionContext context) {
        return "    jobject array_value = stack[--sp].l;\n"
                + "    if (array_value == NULL) {\n"
                + "        throw_npe(env, \"arraylength on null\");\n"
                + "        stack[sp++].i = 0;\n"
                + "    } else {\n"
                + "        stack[sp++].i = (*env)->GetArrayLength(env, (jarray)array_value);\n"
                + "        (*env)->DeleteLocalRef(env, array_value);\n"
                + "    }\n"
                + context.exceptionHandler().apply(context.instructionIndex());
    }
}
