package org.nativeobfuscator.generator.ir.pass;

import org.nativeobfuscator.generator.ir.IrOperation;
import org.nativeobfuscator.generator.ir.IrPass;
import org.nativeobfuscator.generator.ir.IrStage;
import org.nativeobfuscator.generator.ir.NativeMethodIr;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces literal FindClass calls with encrypted lookup descriptors plus a
 * harmless decoy name. Runtime behavior remains identical: the helper decodes
 * and resolves the original class.
 */
public final class ClassLookupIndirectionPass implements IrPass {
    private static final Pattern FIND_CLASS = Pattern.compile(
            "\\(\\*env\\)->FindClass\\(env, \\\"([^\\\"]+)\\\"\\)");
    private static final String[] DECOYS = {
            "java/lang/Object",
            "java/lang/String",
            "java/util/Objects",
            "java/lang/Number"
    };

    @Override
    public IrStage stage() {
        return IrStage.OBFUSCATE;
    }

    @Override
    public String name() {
        return "class-lookup-indirection";
    }

    @Override
    public NativeMethodIr apply(NativeMethodIr method) {
        List<IrOperation> rewritten = new ArrayList<>(method.operations().size());
        for (IrOperation operation : method.operations()) {
            Matcher matcher = FIND_CLASS.matcher(operation.code());
            StringBuffer code = new StringBuffer();
            while (matcher.find()) {
                String className = matcher.group(1);
                int key = nonZeroByte(method.seed() ^ operation.sourceIndex() ^ className.hashCode());
                String replacement = lookupExpression(className, key, method.seed() + operation.sourceIndex());
                matcher.appendReplacement(code, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(code);
            rewritten.add(operation.withCode(code.toString()));
        }
        return method.withOperations(rewritten);
    }

    private String lookupExpression(String className, int key, int decoySeed) {
        byte[] value = className.getBytes(StandardCharsets.UTF_8);
        StringBuilder bytes = new StringBuilder("(const unsigned char[]){");
        for (int index = 0; index < value.length; index++) {
            if (index > 0) {
                bytes.append(',');
            }
            bytes.append(String.format("0x%02X", (value[index] ^ key) & 0xFF));
        }
        bytes.append('}');
        String decoy = DECOYS[Math.floorMod(decoySeed, DECOYS.length)];
        return "jnic_find_class_indirect(env, " + bytes + ", " + value.length
                + ", " + key + ", \"" + decoy + "\")";
    }

    private int nonZeroByte(int value) {
        int result = value & 0xFF;
        return result == 0 ? 0xA7 : result;
    }
}
