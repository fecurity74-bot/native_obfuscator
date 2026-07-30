package org.nativeobfuscator.generator.ir;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class IrPipeline {
    private final List<IrPass> passes;

    public IrPipeline(List<IrPass> passes) {
        this.passes = new ArrayList<>(passes);
        this.passes.sort(Comparator.comparing(IrPass::stage));
    }

    public NativeMethodIr process(NativeMethodIr input) {
        NativeMethodIr current = input;
        for (IrPass pass : passes) {
            current = pass.apply(current);
            if (current == null) {
                throw new IllegalStateException("IR pass returned null: " + pass.name());
            }
        }
        return current;
    }

    public String lower(NativeMethodIr input) {
        NativeMethodIr lowered = process(input);
        StringBuilder result = new StringBuilder();
        for (IrOperation operation : lowered.operations()) {
            result.append(operation.code());
        }
        return result.toString();
    }
}
