package org.nativeobfuscator.generator.ir.pass;

import org.nativeobfuscator.generator.ir.IrOperation;
import org.nativeobfuscator.generator.ir.IrPass;
import org.nativeobfuscator.generator.ir.IrStage;
import org.nativeobfuscator.generator.ir.NativeMethodIr;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative source-level cleanup. It deliberately avoids folding Java
 * numeric operations because C overflow and NaN rules differ from the JVM.
 */
public final class PeepholeOptimizationPass implements IrPass {
    private static final Pattern DIRECT_GOTO = Pattern.compile(
            "\\s*\\{ int sp = \\d+;\\s*goto L(\\d+);\\s*}\\s*",
            Pattern.DOTALL);

    @Override
    public IrStage stage() {
        return IrStage.OPTIMIZE;
    }

    @Override
    public String name() {
        return "safe-peephole";
    }

    @Override
    public NativeMethodIr apply(NativeMethodIr method) {
        List<IrOperation> optimized = new ArrayList<>(method.operations().size());
        for (int index = 0; index < method.operations().size(); index++) {
            IrOperation operation = method.operations().get(index);
            String code = operation.code()
                    .replace("    sp += 1;\n", "    sp++;\n")
                    .replace("    sp -= 1;\n", "    sp--;\n")
                    .replaceAll("\\n{3,}", "\n\n");
            if (isGotoToNextLabel(code, method.operations(), index + 1)) {
                continue;
            }
            optimized.add(operation.withCode(code));
        }
        return method.withOperations(optimized);
    }

    private boolean isGotoToNextLabel(
            String code,
            List<IrOperation> operations,
            int nextIndex) {
        Matcher gotoMatcher = DIRECT_GOTO.matcher(code);
        if (!gotoMatcher.matches()) {
            return false;
        }
        while (nextIndex < operations.size()) {
            IrOperation next = operations.get(nextIndex++);
            if (next.code().isBlank()) {
                continue;
            }
            return next.kind() == IrOperation.Kind.LABEL
                    && next.code().equals("L" + gotoMatcher.group(1) + ":;\n");
        }
        return false;
    }
}
