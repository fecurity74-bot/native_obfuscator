package org.nativeobfuscator.generator.ir;

import java.util.List;
import java.util.Objects;

/**
 * Compact, C-oriented IR used between JVM bytecode analysis and source
 * lowering. It intentionally retains the source opcode/index for diagnostics
 * and deterministic obfuscation.
 */
public record NativeMethodIr(
        String owner,
        String name,
        String descriptor,
        String functionName,
        int seed,
        List<IrOperation> operations) {

    public NativeMethodIr {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(functionName, "functionName");
        operations = List.copyOf(operations);
    }

    public NativeMethodIr withOperations(List<IrOperation> replacement) {
        return new NativeMethodIr(owner, name, descriptor, functionName, seed, replacement);
    }
}
