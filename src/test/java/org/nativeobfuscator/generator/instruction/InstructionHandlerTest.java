package org.nativeobfuscator.generator.instruction;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InstructionHandlerTest {
    @Test
    void dup2X1HandlesThreeCategoryOneValues() {
        Frame<BasicValue> frame = new Frame<>(0, 8);
        frame.push(BasicValue.INT_VALUE);
        frame.push(BasicValue.REFERENCE_VALUE);
        frame.push(BasicValue.INT_VALUE);
        InstructionContext context = context(frame, Map.of());

        String code = new StackInstructionHandler().generate(new InsnNode(Opcodes.DUP2_X1), context);

        assertTrue(code.contains("stack[sp-3] = d21_v2"));
        assertTrue(code.contains("NewLocalRef"));
        assertTrue(code.contains("sp += 2"));
    }

    @Test
    void lookupSwitchUsesStableMethodLabelIds() {
        LabelNode fallback = new LabelNode();
        LabelNode selected = new LabelNode();
        LookupSwitchInsnNode instruction = new LookupSwitchInsnNode(
                fallback, new int[] { 42 }, new LabelNode[] { selected });
        InstructionContext context = context(new Frame<>(0, 1),
                Map.of(fallback, 10, selected, 20));

        String code = new SwitchInstructionHandler().generate(instruction, context);

        assertTrue(code.contains("case 42: goto L20"));
        assertTrue(code.contains("default: goto L10"));
    }

    private InstructionContext context(Frame<BasicValue> frame, Map<LabelNode, Integer> labels) {
        return new InstructionContext(frame, frame.getStackSize(), 7, Type.VOID_TYPE,
                labels, ignored -> "");
    }
}
