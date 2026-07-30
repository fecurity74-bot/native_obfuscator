package org.nativeobfuscator.generator.instruction;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvocationInstructionHandlerTest {
    @Test
    void invokeSpecialUsesNonVirtualDispatch() {
        assertEquals("CallNonvirtualIntMethodA",
                InvocationInstructionHandler.callFunction(Opcodes.INVOKESPECIAL, "Int"));
        assertTrue(InvocationInstructionHandler.requiresOwnerClassArgument(Opcodes.INVOKESPECIAL));
    }

    @Test
    void interfaceInvocationKeepsVirtualDispatch() {
        assertEquals("CallObjectMethodA",
                InvocationInstructionHandler.callFunction(Opcodes.INVOKEINTERFACE, "Object"));
        assertFalse(InvocationInstructionHandler.requiresOwnerClassArgument(Opcodes.INVOKEINTERFACE));
    }
}
