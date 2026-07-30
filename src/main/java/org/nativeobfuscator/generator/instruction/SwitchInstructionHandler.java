package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

final class SwitchInstructionHandler implements InstructionHandler {
    @Override
    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        StringBuilder code = new StringBuilder();
        code.append("    switch (stack[--sp].i) {\n");
        if (instruction.getOpcode() == Opcodes.TABLESWITCH) {
            TableSwitchInsnNode table = (TableSwitchInsnNode) instruction;
            for (int i = 0; i < table.labels.size(); i++) {
                code.append("        case ").append(table.min + i).append(": goto L")
                        .append(context.labelId(table.labels.get(i))).append(";\n");
            }
            code.append("        default: goto L").append(context.labelId(table.dflt)).append(";\n");
        } else if (instruction.getOpcode() == Opcodes.LOOKUPSWITCH) {
            LookupSwitchInsnNode lookup = (LookupSwitchInsnNode) instruction;
            for (int i = 0; i < lookup.labels.size(); i++) {
                code.append("        case ").append(lookup.keys.get(i)).append(": goto L")
                        .append(context.labelId(lookup.labels.get(i))).append(";\n");
            }
            code.append("        default: goto L").append(context.labelId(lookup.dflt)).append(";\n");
        } else {
            throw new IllegalArgumentException("Unsupported switch opcode");
        }
        code.append("    }\n");
        return code.toString();
    }
}
