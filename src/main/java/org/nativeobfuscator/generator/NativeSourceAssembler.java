package org.nativeobfuscator.generator;

import org.nativeobfuscator.generator.template.CTemplateRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class NativeSourceAssembler {
    private final CTemplateRepository templates;

    public NativeSourceAssembler(CTemplateRepository templates) {
        this.templates = templates;
    }

    public String assemble(
            CharSequence preamble,
            CharSequence prototypes,
            CharSequence methods,
            Collection<NativeMethodBinding> bindings) {
        String registration = templates.render("registration.c", Map.of(
                "CLASS_REGISTRATION", classRegistration(bindings)));
        return templates.render("source.c", Map.of(
                "PREAMBLE", preamble,
                "PROTOTYPES", prototypes,
                "METHODS", methods,
                "REGISTRATION", registration));
    }

    private String classRegistration(Collection<NativeMethodBinding> bindings) {
        Map<String, List<NativeMethodBinding>> groups = new TreeMap<>();
        for (NativeMethodBinding binding : bindings) {
            groups.computeIfAbsent(binding.className(), ignored -> new ArrayList<>())
                    .add(binding);
        }

        StringBuilder code = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, List<NativeMethodBinding>> group : groups.entrySet()) {
            List<NativeMethodBinding> classBindings = group.getValue();
            classBindings.sort((left, right) -> {
                int name = left.methodName().compareTo(right.methodName());
                return name != 0 ? name : left.signature().compareTo(right.signature());
            });

            String dotName = group.getKey().replace('/', '.');
            String safeClassName = group.getKey().replace('/', '_').replace('$', '_');
            code.append(first ? "    if" : "    else if")
                    .append(" (strcmp(class_name, \"").append(escape(dotName))
                    .append("\") == 0) {\n")
                    .append("        static const JNINativeMethod methods_")
                    .append(safeClassName).append("[] = {\n");
            first = false;

            for (NativeMethodBinding binding : classBindings) {
                code.append("            {\"").append(escape(binding.methodName()))
                        .append("\", \"").append(escape(binding.signature()))
                        .append("\", (void *)&").append(binding.cFunctionName())
                        .append("},\n");
            }
            code.append("        };\n")
                    .append("        if ((*env)->RegisterNatives(env, target, methods_")
                    .append(safeClassName).append(", ")
                    .append(classBindings.size()).append(") != JNI_OK) {\n")
                    .append("            (*env)->ReleaseStringUTFChars(env, name_string, class_name);\n")
                    .append("            (*env)->DeleteLocalRef(env, name_string);\n")
                    .append("            (*env)->DeleteLocalRef(env, class_class);\n")
                    .append("            return;\n")
                    .append("        }\n")
                    .append("    }\n");
        }
        return code.toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
