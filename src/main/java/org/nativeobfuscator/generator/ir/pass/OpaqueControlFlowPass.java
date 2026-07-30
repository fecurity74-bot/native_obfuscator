package org.nativeobfuscator.generator.ir.pass;

import org.nativeobfuscator.generator.ir.IrOperation;
import org.nativeobfuscator.generator.ir.IrPass;
import org.nativeobfuscator.generator.ir.IrStage;
import org.nativeobfuscator.generator.ir.NativeMethodIr;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds deterministic opaque predicates around self-contained instruction
 * emissions. Labels and terminal control transfers are left untouched so the
 * generated C control-flow graph remains valid.
 */
public final class OpaqueControlFlowPass implements IrPass {
    @Override
    public IrStage stage() {
        return IrStage.OBFUSCATE;
    }

    @Override
    public String name() {
        return "opaque-control-flow";
    }

    @Override
    public NativeMethodIr apply(NativeMethodIr method) {
        List<IrOperation> result = new ArrayList<>(method.operations().size() + 1);
        int entryToken = mix(method.seed());
        result.add(IrOperation.synthetic(
                "    volatile uint32_t ir_token = 0x" + Integer.toHexString(entryToken) + "u;\n"
                        + "    ir_token ^= (ir_token << 7) | (ir_token >> 25);\n"
                        + "    (void)ir_token;\n"));

        for (IrOperation operation : method.operations()) {
            if (operation.kind() != IrOperation.Kind.INSTRUCTION
                    || isTerminal(operation.code())
                    || operation.code().contains("goto L")) {
                result.add(operation);
                continue;
            }
            int token = mix(method.seed() ^ operation.sourceIndex() ^ operation.opcode());
            String wrapped = "    if (jnic_opaque_true(0x" + Integer.toHexString(token) + "u)) {\n"
                    + indent(operation.code())
                    + "    } else {\n"
                    + "        jnic_lifter_decoy(env, 0x" + Integer.toHexString(token ^ 0x6D2B79F5) + "u);\n"
                    + "    }\n";
            result.add(operation.withCode(wrapped));
        }
        return method.withOperations(result);
    }

    private boolean isTerminal(String code) {
        return code.contains("return;")
                || code.contains("return stack[")
                || code.contains("return (*env)")
                || code.contains("return 0;");
    }

    private String indent(String code) {
        return "    " + code.replace("\n", "\n    ").stripTrailing() + "\n";
    }

    private int mix(int value) {
        int mixed = value;
        mixed ^= mixed >>> 16;
        mixed *= 0x7FEB352D;
        mixed ^= mixed >>> 15;
        mixed *= 0x846CA68B;
        mixed ^= mixed >>> 16;
        return mixed;
    }
}
