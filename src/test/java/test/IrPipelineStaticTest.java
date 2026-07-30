package test;

import org.junit.jupiter.api.Test;
import org.nativeobfuscator.generator.ir.IrOperation;
import org.nativeobfuscator.generator.ir.IrPipeline;
import org.nativeobfuscator.generator.ir.NativeMethodIr;
import org.nativeobfuscator.generator.ir.pass.ClassLookupIndirectionPass;
import org.nativeobfuscator.generator.ir.pass.LoweringMetadataPass;
import org.nativeobfuscator.generator.ir.pass.NormalizeIrPass;
import org.nativeobfuscator.generator.ir.pass.OpaqueControlFlowPass;
import org.nativeobfuscator.generator.ir.pass.PeepholeOptimizationPass;
import org.objectweb.asm.Opcodes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IrPipelineStaticTest {
    @Test
    void pipelineIsDeterministicAndHidesLiteralClassLookup() {
        NativeMethodIr method = new NativeMethodIr(
                "sample/Owner",
                "method",
                "()V",
                "native_method",
                0x12345678,
                List.of(IrOperation.instruction(
                        4,
                        Opcodes.INSTANCEOF,
                        "    jclass type = (*env)->FindClass(env, \"sample/Secret\");\n")));
        IrPipeline pipeline = new IrPipeline(List.of(
                new NormalizeIrPass(),
                new PeepholeOptimizationPass(),
                new ClassLookupIndirectionPass(),
                new OpaqueControlFlowPass(),
                new LoweringMetadataPass()));

        String first = pipeline.lower(method);
        String second = pipeline.lower(method);

        assertEquals(first, second);
        assertTrue(first.contains("jnic_find_class_indirect"));
        assertTrue(first.contains("jnic_opaque_true"));
        assertFalse(first.contains("\"sample/Secret\""));
    }
}
