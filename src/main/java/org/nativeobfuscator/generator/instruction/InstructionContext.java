package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.Map;
import java.util.function.IntFunction;

public record InstructionContext(
        Frame<BasicValue> frame,
        int stackPointer,
        int instructionIndex,
        Type methodReturnType,
        Map<LabelNode, Integer> labels,
        IntFunction<String> exceptionHandler) {

    public int labelId(LabelNode label) {
        Integer id = labels.get(label);
        if (id == null) {
            throw new IllegalArgumentException("Instruction references a label outside the method");
        }
        return id;
    }

    public boolean isReference(BasicValue value) {
        if (value == null || value.getType() == null) {
            return false;
        }
        int sort = value.getType().getSort();
        return sort == Type.OBJECT || sort == Type.ARRAY;
    }
}
