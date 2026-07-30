package org.nativeobfuscator.generator.ir;

public interface IrPass {
    IrStage stage();

    String name();

    NativeMethodIr apply(NativeMethodIr method);
}
