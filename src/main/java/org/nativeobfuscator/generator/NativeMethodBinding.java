package org.nativeobfuscator.generator;

import java.util.Objects;

public record NativeMethodBinding(
        String className,
        String methodName,
        String signature,
        String cFunctionName,
        boolean staticMethod) {

    public NativeMethodBinding {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(signature, "signature");
        Objects.requireNonNull(cFunctionName, "cFunctionName");
    }
}
