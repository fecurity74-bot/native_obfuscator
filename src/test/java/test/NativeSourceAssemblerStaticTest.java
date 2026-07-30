package test;

import org.junit.jupiter.api.Test;
import org.nativeobfuscator.generator.NativeMethodBinding;
import org.nativeobfuscator.generator.NativeSourceAssembler;
import org.nativeobfuscator.generator.template.CTemplateRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeSourceAssemblerStaticTest {
    @Test
    void sourceContainsHiddenMethodAndManualRegistrationTable() {
        NativeSourceAssembler assembler =
                new NativeSourceAssembler(new CTemplateRepository());
        String source = assembler.assemble(
                new CTemplateRepository().load("preamble.c"),
                "static JNIC_NO_OPT jint JNICALL fn(JNIEnv*, jobject);\n",
                "static JNIC_NO_OPT jint JNICALL fn(JNIEnv *env, jobject self) { return 1; }\n",
                List.of(new NativeMethodBinding(
                        "sample/Owner", "value", "()I", "fn", false)));

        assertTrue(source.contains("methods_sample_Owner"));
        assertTrue(source.contains("(void *)&fn"));
        assertTrue(source.contains("JNI_OnLoad"));
        assertFalse(source.contains("Java_sample_Owner_value"));
    }
}
