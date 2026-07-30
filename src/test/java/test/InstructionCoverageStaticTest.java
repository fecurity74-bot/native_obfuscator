package test;

import org.junit.jupiter.api.Test;
import org.nativeobfuscator.generator.instruction.InstructionDispatcher;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InstructionCoverageStaticTest {
    @Test
    void objectAndArrayOpcodesHaveDedicatedHandlers() {
        InstructionDispatcher dispatcher = new InstructionDispatcher();

        assertTrue(dispatcher.supports(Opcodes.NEWARRAY));
        assertTrue(dispatcher.supports(Opcodes.ANEWARRAY));
        assertTrue(dispatcher.supports(Opcodes.ARRAYLENGTH));
        assertTrue(dispatcher.supports(Opcodes.CHECKCAST));
        assertTrue(dispatcher.supports(Opcodes.INSTANCEOF));
        assertTrue(dispatcher.supports(Opcodes.ATHROW));
    }
}
