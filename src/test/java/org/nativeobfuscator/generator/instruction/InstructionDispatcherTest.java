package org.nativeobfuscator.generator.instruction;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InstructionDispatcherTest {
    @Test
    void coversPreviouslyMissingInstructions() {
        InstructionDispatcher dispatcher = new InstructionDispatcher();
        assertTrue(dispatcher.supports(Opcodes.DUP2_X1));
        assertTrue(dispatcher.supports(Opcodes.DUP2_X2));
        assertTrue(dispatcher.supports(Opcodes.TABLESWITCH));
        assertTrue(dispatcher.supports(Opcodes.LOOKUPSWITCH));
        assertTrue(dispatcher.supports(Opcodes.MULTIANEWARRAY));
    }

    @Test
    void declaresCompleteModernJvmCoverage() {
        assertTrue(OpcodeSupport.count() >= 150);
        assertTrue(OpcodeSupport.isSupported(Opcodes.INVOKEDYNAMIC));
        assertTrue(OpcodeSupport.isSupported(Opcodes.MONITOREXIT));
    }
}
