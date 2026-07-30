package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

final class MonitorInstructionHandler implements InstructionHandler {
    @Override
    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        int id = context.instructionIndex();
        String call = instruction.getOpcode() == Opcodes.MONITORENTER ? "MonitorEnter" : "MonitorExit";
        return "    jobject monitor_" + id + " = stack[--sp].l;\n"
                + "    if (monitor_" + id + " == NULL) {\n"
                + "        throw_npe(env, \"monitor operation on null\");\n"
                + "    } else {\n"
                + "        (*env)->" + call + "(env, monitor_" + id + ");\n"
                + "    }\n"
                + context.exceptionHandler().apply(context.instructionIndex());
    }
}
