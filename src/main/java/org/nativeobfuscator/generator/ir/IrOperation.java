package org.nativeobfuscator.generator.ir;

import java.util.Objects;

public record IrOperation(int sourceIndex, int opcode, Kind kind, String code) {
    public enum Kind {
        LABEL,
        INSTRUCTION,
        SYNTHETIC
    }

    public IrOperation {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(code, "code");
    }

    public IrOperation withCode(String replacement) {
        return new IrOperation(sourceIndex, opcode, kind, replacement);
    }

    public static IrOperation label(int sourceIndex, int labelId) {
        return new IrOperation(sourceIndex, -1, Kind.LABEL, "L" + labelId + ":;\n");
    }

    public static IrOperation instruction(int sourceIndex, int opcode, String code) {
        return new IrOperation(sourceIndex, opcode, Kind.INSTRUCTION, code);
    }

    public static IrOperation synthetic(String code) {
        return new IrOperation(-1, -1, Kind.SYNTHETIC, code);
    }
}
