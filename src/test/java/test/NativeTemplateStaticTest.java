package test;

import org.junit.jupiter.api.Test;
import org.nativeobfuscator.generator.template.CTemplateRepository;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeTemplateStaticTest {
    private final CTemplateRepository templates = new CTemplateRepository();

    @Test
    void generatedMethodsUseResourceTemplateWithoutJniExport() {
        String method = templates.render("method.c", Map.of(
                "DECLARATION", "static JNIC_NO_OPT jint JNICALL generated(JNIEnv *env, jobject thiz)",
                "PROLOGUE", "",
                "BODY", "    return 1;\n",
                "EPILOGUE", ""));

        assertTrue(method.contains("static JNIC_NO_OPT"));
        assertFalse(method.contains("JNIEXPORT"));
    }

    @Test
    void loaderBridgeIsRegisteredManuallyFromOnLoad() {
        String registration = templates.render("registration.c", Map.of(
                "CLASS_REGISTRATION", ""));

        assertTrue(registration.contains("RegisterNatives"));
        assertTrue(registration.contains("\"registerNatives\""));
        assertFalse(registration.contains(
                "Java_org_nativeobfuscator_NativeLoader_registerNatives"));
    }

    @Test
    void onlyGeneratedFunctionsCarryNoOptimizationAttribute() {
        String preamble = templates.load("preamble.c");
        String registration = templates.render("registration.c", Map.of(
                "CLASS_REGISTRATION", ""));

        assertTrue(preamble.contains("#define JNIC_NO_OPT"));
        assertFalse(registration.contains("JNIC_NO_OPT"));
    }
}
