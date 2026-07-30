static void JNICALL jnic_register_target(JNIEnv *env, jclass loader, jclass target) {
    (void)loader;
    if (target == NULL) {
        return;
    }

    jclass class_class = (*env)->GetObjectClass(env, target);
    if (class_class == NULL) {
        return;
    }
    jmethodID get_name = (*env)->GetMethodID(
        env, class_class, "getName", "()Ljava/lang/String;");
    if (get_name == NULL) {
        (*env)->DeleteLocalRef(env, class_class);
        return;
    }

    jstring name_string = (jstring)(*env)->CallObjectMethod(env, target, get_name);
    if (name_string == NULL) {
        (*env)->DeleteLocalRef(env, class_class);
        return;
    }
    const char *class_name = (*env)->GetStringUTFChars(env, name_string, NULL);
    if (class_name == NULL) {
        (*env)->DeleteLocalRef(env, name_string);
        (*env)->DeleteLocalRef(env, class_class);
        return;
    }

{{CLASS_REGISTRATION}}

    (*env)->ReleaseStringUTFChars(env, name_string, class_name);
    (*env)->DeleteLocalRef(env, name_string);
    (*env)->DeleteLocalRef(env, class_class);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)reserved;
    JNIEnv *env = NULL;
    g_jvm = vm;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    init_global_cache(env);

    jclass loader = (*env)->FindClass(env, "org/nativeobfuscator/NativeLoader");
    if (loader == NULL) {
        return JNI_ERR;
    }
    JNINativeMethod loader_methods[] = {
        {
            "registerNatives",
            "(Ljava/lang/Class;)V",
            (void *)&jnic_register_target
        }
    };
    if ((*env)->RegisterNatives(env, loader, loader_methods, 1) != JNI_OK) {
        (*env)->DeleteLocalRef(env, loader);
        return JNI_ERR;
    }
    (*env)->DeleteLocalRef(env, loader);
    return JNI_VERSION_1_6;
}
