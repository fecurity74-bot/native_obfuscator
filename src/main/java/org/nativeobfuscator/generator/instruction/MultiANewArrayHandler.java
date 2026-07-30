package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;

final class MultiANewArrayHandler implements InstructionHandler {
    @Override
    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        MultiANewArrayInsnNode array = (MultiANewArrayInsnNode) instruction;
        Type element = Type.getType(array.desc).getElementType();
        String id = Integer.toString(context.instructionIndex());
        StringBuilder code = new StringBuilder();

        code.append("    jint dims_").append(id).append("[").append(array.dims).append("] = {");
        for (int i = 0; i < array.dims; i++) {
            if (i > 0) {
                code.append(", ");
            }
            code.append("stack[sp-").append(array.dims - i).append("].i");
        }
        code.append("};\n");
        code.append("    sp -= ").append(array.dims).append(";\n");
        code.append("    jintArray jdims_").append(id).append(" = (*env)->NewIntArray(env, ")
                .append(array.dims).append(");\n");
        code.append("    if (jdims_").append(id).append(" != NULL) {\n");
        code.append("        (*env)->SetIntArrayRegion(env, jdims_").append(id)
                .append(", 0, ").append(array.dims).append(", dims_").append(id).append(");\n");
        code.append("    }\n");
        code.append("    jclass component_").append(id).append(" = NULL;\n");
        appendComponentClass(code, element, id);
        code.append("    jclass reflect_array_").append(id)
                .append(" = (*env)->FindClass(env, \"java/lang/reflect/Array\");\n");
        code.append("    jmethodID new_instance_").append(id).append(" = reflect_array_").append(id)
                .append(" == NULL ? NULL : (*env)->GetStaticMethodID(env, reflect_array_").append(id)
                .append(", \"newInstance\", \"(Ljava/lang/Class;[I)Ljava/lang/Object;\");\n");
        code.append("    jobject result_").append(id).append(" = NULL;\n");
        code.append("    if (component_").append(id).append(" != NULL && jdims_").append(id)
                .append(" != NULL && new_instance_").append(id).append(" != NULL) {\n");
        code.append("        result_").append(id).append(" = (*env)->CallStaticObjectMethod(env, reflect_array_")
                .append(id).append(", new_instance_").append(id).append(", component_").append(id)
                .append(", jdims_").append(id).append(");\n");
        code.append("    }\n");
        code.append("    stack[sp++].l = result_").append(id).append(";\n");
        code.append(context.exceptionHandler().apply(context.instructionIndex()));
        return code.toString();
    }

    private void appendComponentClass(StringBuilder code, Type element, String id) {
        if (element.getSort() == Type.OBJECT) {
            code.append("    component_").append(id).append(" = (*env)->FindClass(env, \"")
                    .append(element.getInternalName()).append("\");\n");
            return;
        }
        String wrapper = switch (element.getSort()) {
            case Type.BOOLEAN -> "java/lang/Boolean";
            case Type.BYTE -> "java/lang/Byte";
            case Type.CHAR -> "java/lang/Character";
            case Type.SHORT -> "java/lang/Short";
            case Type.INT -> "java/lang/Integer";
            case Type.FLOAT -> "java/lang/Float";
            case Type.LONG -> "java/lang/Long";
            case Type.DOUBLE -> "java/lang/Double";
            default -> throw new IllegalArgumentException("Invalid MULTIANEWARRAY element type");
        };
        code.append("    jclass wrapper_").append(id).append(" = (*env)->FindClass(env, \"")
                .append(wrapper).append("\");\n");
        code.append("    if (wrapper_").append(id).append(" != NULL) {\n");
        code.append("        jfieldID type_").append(id)
                .append(" = (*env)->GetStaticFieldID(env, wrapper_").append(id)
                .append(", \"TYPE\", \"Ljava/lang/Class;\");\n");
        code.append("        if (type_").append(id).append(" != NULL) component_").append(id)
                .append(" = (jclass)(*env)->GetStaticObjectField(env, wrapper_").append(id)
                .append(", type_").append(id).append(");\n");
        code.append("    }\n");
    }
}
