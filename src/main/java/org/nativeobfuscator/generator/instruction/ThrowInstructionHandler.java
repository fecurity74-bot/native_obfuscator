package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.tree.AbstractInsnNode;

final class ThrowInstructionHandler implements InstructionHandler {
    @Override
    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        return "    jthrowable thrown = (jthrowable)stack[--sp].l;\n"
                + "    if (thrown == NULL) {\n"
                + "        throw_npe(env, \"athrow with null\");\n"
                + "    } else {\n"
                + "        (*env)->Throw(env, thrown);\n"
                + "        (*env)->DeleteLocalRef(env, thrown);\n"
                + "    }\n"
                + context.exceptionHandler().apply(context.instructionIndex());
    }
}
