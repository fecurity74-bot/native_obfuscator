package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.tree.AbstractInsnNode;

public interface InstructionHandler {
    String generate(AbstractInsnNode instruction, InstructionContext context);
}
