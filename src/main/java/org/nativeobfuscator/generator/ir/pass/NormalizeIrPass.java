package org.nativeobfuscator.generator.ir.pass;

import org.nativeobfuscator.generator.ir.IrOperation;
import org.nativeobfuscator.generator.ir.IrPass;
import org.nativeobfuscator.generator.ir.IrStage;
import org.nativeobfuscator.generator.ir.NativeMethodIr;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

public final class NormalizeIrPass implements IrPass {
    @Override
    public IrStage stage() {
        return IrStage.NORMALIZE;
    }

    @Override
    public String name() {
        return "normalize";
    }

    @Override
    public NativeMethodIr apply(NativeMethodIr method) {
        List<IrOperation> normalized = new ArrayList<>(method.operations().size());
        for (IrOperation operation : method.operations()) {
            if (operation.kind() == IrOperation.Kind.INSTRUCTION
                    && (operation.opcode() == Opcodes.NOP || operation.code().isBlank())) {
                continue;
            }
            normalized.add(operation.withCode(operation.code().replace("\r\n", "\n")));
        }
        return method.withOperations(normalized);
    }
}
