package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

final class TypeCheckInstructionHandler implements InstructionHandler {
    @Override
    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        TypeInsnNode type = (TypeInsnNode) instruction;
        String id = Integer.toUnsignedString(context.instructionIndex());
        return switch (instruction.getOpcode()) {
            case Opcodes.CHECKCAST -> checkCast(type.desc, id, context);
            case Opcodes.INSTANCEOF -> instanceOf(type.desc, id, context);
            default -> throw new IllegalArgumentException("Unsupported type-check opcode");
        };
    }

    private String checkCast(String descriptor, String id, InstructionContext context) {
        return "    if (stack[sp-1].l != NULL) {\n"
                + "        jclass cast_type_" + id + " = (*env)->FindClass(env, \""
                + descriptor + "\");\n"
                + "        if (cast_type_" + id + " != NULL\n"
                + "                && !(*env)->IsInstanceOf(env, stack[sp-1].l, cast_type_"
                + id + ")) {\n"
                + "            jclass cast_error_" + id
                + " = (*env)->FindClass(env, \"java/lang/ClassCastException\");\n"
                + "            if (cast_error_" + id + " != NULL) {\n"
                + "                (*env)->ThrowNew(env, cast_error_" + id
                + ", \"native CHECKCAST failed\");\n"
                + "                (*env)->DeleteLocalRef(env, cast_error_" + id + ");\n"
                + "            }\n"
                + "        }\n"
                + "        if (cast_type_" + id + " != NULL) "
                + "(*env)->DeleteLocalRef(env, cast_type_" + id + ");\n"
                + "    }\n"
                + context.exceptionHandler().apply(context.instructionIndex());
    }

    private String instanceOf(String descriptor, String id, InstructionContext context) {
        return "    jobject instance_" + id + " = stack[--sp].l;\n"
                + "    if (instance_" + id + " == NULL) {\n"
                + "        stack[sp++].i = 0;\n"
                + "    } else {\n"
                + "        jclass instance_type_" + id + " = (*env)->FindClass(env, \""
                + descriptor + "\");\n"
                + "        stack[sp++].i = instance_type_" + id + " == NULL ? 0\n"
                + "            : (*env)->IsInstanceOf(env, instance_" + id
                + ", instance_type_" + id + ");\n"
                + "        if (instance_type_" + id + " != NULL) "
                + "(*env)->DeleteLocalRef(env, instance_type_" + id + ");\n"
                + "        (*env)->DeleteLocalRef(env, instance_" + id + ");\n"
                + "    }\n"
                + context.exceptionHandler().apply(context.instructionIndex());
    }
}
