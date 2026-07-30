package org.nativeobfuscator.generator.ir.pass;

import org.nativeobfuscator.generator.ir.IrOperation;
import org.nativeobfuscator.generator.ir.IrPass;
import org.nativeobfuscator.generator.ir.IrStage;
import org.nativeobfuscator.generator.ir.NativeMethodIr;

import java.util.ArrayList;
import java.util.List;

public final class LoweringMetadataPass implements IrPass {
    @Override
    public IrStage stage() {
        return IrStage.LOWER;
    }

    @Override
    public String name() {
        return "lowering-metadata";
    }

    @Override
    public NativeMethodIr apply(NativeMethodIr method) {
        List<IrOperation> result = new ArrayList<>(method.operations().size() + 1);
        result.add(IrOperation.synthetic("    /* lowered IR "
                + Integer.toUnsignedString(method.seed(), 16) + " */\n"));
        result.addAll(method.operations());
        return method.withOperations(result);
    }
}
