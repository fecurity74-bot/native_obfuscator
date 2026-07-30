#include "jni.h"
#include <math.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#if defined(__clang__)
#define JNIC_NO_OPT __attribute__((optnone, noinline))
#elif defined(__GNUC__)
#define JNIC_NO_OPT __attribute__((optimize("O0"), noinline))
#elif defined(_MSC_VER)
#define JNIC_NO_OPT __declspec(noinline)
#else
#define JNIC_NO_OPT
#endif

#ifndef JNIC_DEBUG
#define JNIC_DEBUG 0
#endif

typedef union {
    jint i;
    jlong j;
    jfloat f;
    jdouble d;
    jobject l;
} StackValue;

#if JNIC_DEBUG
static void log_debug(const char *format, ...) {
    FILE *file = fopen("native_debug.log", "a");
    if (file == NULL) return;
    va_list arguments;
    va_start(arguments, format);
    vfprintf(file, format, arguments);
    va_end(arguments);
    fclose(file);
}
#else
static void log_debug(const char *format, ...) {
    (void)format;
}
#endif

static JavaVM *g_jvm = NULL;
static jclass g_cls_String = NULL;
static jclass g_cls_StringBuilder = NULL;
static jclass g_cls_Object = NULL;
static jclass g_cls_Class = NULL;
static jclass g_cls_System = NULL;
static jclass g_cls_Math = NULL;
static jclass g_cls_Arrays = NULL;
static jclass g_cls_NullPointerException = NULL;
static jclass g_cls_ArrayIndexOutOfBoundsException = NULL;
static jclass g_cls_ArithmeticException = NULL;
static jclass g_cls_ClassCastException = NULL;

static jmethodID g_mid_String_length = NULL;
static jmethodID g_mid_String_hashCode = NULL;
static jmethodID g_mid_String_charAt = NULL;
static jmethodID g_mid_StringBuilder_init = NULL;
static jmethodID g_mid_StringBuilder_append_String = NULL;
static jmethodID g_mid_StringBuilder_append_int = NULL;
static jmethodID g_mid_StringBuilder_append_long = NULL;
static jmethodID g_mid_StringBuilder_append_double = NULL;
static jmethodID g_mid_StringBuilder_append_Object = NULL;
static jmethodID g_mid_StringBuilder_toString = NULL;
static jmethodID g_mid_Object_getClass = NULL;
static jmethodID g_mid_Object_hashCode = NULL;
static jmethodID g_mid_Object_toString = NULL;
static jmethodID g_mid_Class_getName = NULL;
static jmethodID g_mid_System_arraycopy = NULL;

static int g_cache_initialized = 0;
static int g_cache_initializing = 0;
static volatile uint32_t g_opaque_entropy = 0x9E3779B9u;

static jclass get_or_cache_class(JNIEnv *env, jclass *cache, const char *name) {
    jclass resolved = *cache;
    if (resolved != NULL) return resolved;
    jclass local = (*env)->FindClass(env, name);
    if (local == NULL) return NULL;
    resolved = (*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    *cache = resolved;
    return resolved;
}

static void init_global_cache(JNIEnv *env) {
    if (env == NULL || g_cache_initialized || g_cache_initializing) return;
    g_cache_initializing = 1;
    jclass temporary = NULL;

#define CACHE_CLASS(variable, name) \
    do { \
        if ((variable) == NULL) { \
            temporary = (*env)->FindClass(env, (name)); \
            if (temporary != NULL) { \
                (variable) = (*env)->NewGlobalRef(env, temporary); \
                (*env)->DeleteLocalRef(env, temporary); \
            } else if ((*env)->ExceptionCheck(env)) { \
                (*env)->ExceptionClear(env); \
            } \
        } \
    } while (0)

    CACHE_CLASS(g_cls_String, "java/lang/String");
    CACHE_CLASS(g_cls_StringBuilder, "java/lang/StringBuilder");
    CACHE_CLASS(g_cls_Object, "java/lang/Object");
    CACHE_CLASS(g_cls_Class, "java/lang/Class");
    CACHE_CLASS(g_cls_System, "java/lang/System");
    CACHE_CLASS(g_cls_Math, "java/lang/Math");
    CACHE_CLASS(g_cls_Arrays, "java/util/Arrays");
    CACHE_CLASS(g_cls_NullPointerException, "java/lang/NullPointerException");
    CACHE_CLASS(g_cls_ArrayIndexOutOfBoundsException, "java/lang/ArrayIndexOutOfBoundsException");
    CACHE_CLASS(g_cls_ArithmeticException, "java/lang/ArithmeticException");
    CACHE_CLASS(g_cls_ClassCastException, "java/lang/ClassCastException");
#undef CACHE_CLASS

#define CACHE_METHOD(variable, owner, getter, name, descriptor) \
    do { \
        if ((owner) != NULL && (variable) == NULL) { \
            (variable) = (*env)->getter(env, (owner), (name), (descriptor)); \
            if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env); \
        } \
    } while (0)

    CACHE_METHOD(g_mid_String_length, g_cls_String, GetMethodID, "length", "()I");
    CACHE_METHOD(g_mid_String_hashCode, g_cls_String, GetMethodID, "hashCode", "()I");
    CACHE_METHOD(g_mid_String_charAt, g_cls_String, GetMethodID, "charAt", "(I)C");
    CACHE_METHOD(g_mid_StringBuilder_init, g_cls_StringBuilder, GetMethodID, "<init>", "()V");
    CACHE_METHOD(g_mid_StringBuilder_append_String, g_cls_StringBuilder, GetMethodID,
        "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
    CACHE_METHOD(g_mid_StringBuilder_append_int, g_cls_StringBuilder, GetMethodID,
        "append", "(I)Ljava/lang/StringBuilder;");
    CACHE_METHOD(g_mid_StringBuilder_append_long, g_cls_StringBuilder, GetMethodID,
        "append", "(J)Ljava/lang/StringBuilder;");
    CACHE_METHOD(g_mid_StringBuilder_append_double, g_cls_StringBuilder, GetMethodID,
        "append", "(D)Ljava/lang/StringBuilder;");
    CACHE_METHOD(g_mid_StringBuilder_append_Object, g_cls_StringBuilder, GetMethodID,
        "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
    CACHE_METHOD(g_mid_StringBuilder_toString, g_cls_StringBuilder, GetMethodID,
        "toString", "()Ljava/lang/String;");
    CACHE_METHOD(g_mid_Object_getClass, g_cls_Object, GetMethodID,
        "getClass", "()Ljava/lang/Class;");
    CACHE_METHOD(g_mid_Object_hashCode, g_cls_Object, GetMethodID, "hashCode", "()I");
    CACHE_METHOD(g_mid_Object_toString, g_cls_Object, GetMethodID,
        "toString", "()Ljava/lang/String;");
    CACHE_METHOD(g_mid_Class_getName, g_cls_Class, GetMethodID,
        "getName", "()Ljava/lang/String;");
    CACHE_METHOD(g_mid_System_arraycopy, g_cls_System, GetStaticMethodID,
        "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
#undef CACHE_METHOD

    g_cache_initialized = 1;
    g_cache_initializing = 0;
}

static char *decrypt_string_len(const unsigned char *encrypted, int length, int key) {
    char *result = (char *)malloc((size_t)length + 1u);
    if (result == NULL) return NULL;
    for (int index = 0; index < length; index++) {
        result[index] = (char)(encrypted[index] ^ key);
    }
    result[length] = '\0';
    return result;
}

static jclass jnic_find_class_indirect(
        JNIEnv *env,
        const unsigned char *encoded,
        int length,
        int key,
        const char *decoy) {
    volatile size_t decoy_length = strlen(decoy);
    (void)decoy_length;
    char *name = decrypt_string_len(encoded, length, key);
    if (name == NULL) return NULL;
    jclass result = (*env)->FindClass(env, name);
    memset(name, 0, (size_t)length);
    free(name);
    return result;
}

static int jnic_opaque_true(uint32_t salt) {
    uint32_t value = salt ^ g_opaque_entropy;
    return ((value * value + value) & 1u) == 0u;
}

static void jnic_lifter_decoy(JNIEnv *env, uint32_t token) {
    static const char *decoys[] = {
        "java/lang/Object",
        "java/lang/String",
        "java/util/Objects",
        "java/lang/Number"
    };
    jclass ignored = (*env)->FindClass(env, decoys[token & 3u]);
    if (ignored != NULL) (*env)->DeleteLocalRef(env, ignored);
    if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);
}

static void throw_npe(JNIEnv *env, const char *message);
static void throw_aioobe(JNIEnv *env, const char *message);
static void throw_arith(JNIEnv *env, const char *message);

static jboolean inline_string_equals(JNIEnv *env, jobject left, jobject right) {
    if (left == right) return JNI_TRUE;
    if (left == NULL || right == NULL) return JNI_FALSE;
    init_global_cache(env);
    if (!(*env)->IsInstanceOf(env, right, g_cls_String)) return JNI_FALSE;
    jint left_length = (*env)->GetStringLength(env, (jstring)left);
    jint right_length = (*env)->GetStringLength(env, (jstring)right);
    if (left_length != right_length) return JNI_FALSE;
    const jchar *left_chars = (*env)->GetStringChars(env, (jstring)left, NULL);
    const jchar *right_chars = (*env)->GetStringChars(env, (jstring)right, NULL);
    if (left_chars == NULL || right_chars == NULL) return JNI_FALSE;
    jboolean equal = JNI_TRUE;
    for (jint index = 0; index < left_length; index++) {
        if (left_chars[index] != right_chars[index]) {
            equal = JNI_FALSE;
            break;
        }
    }
    (*env)->ReleaseStringChars(env, (jstring)left, left_chars);
    (*env)->ReleaseStringChars(env, (jstring)right, right_chars);
    return equal;
}

static jint inline_string_length(JNIEnv *env, jstring value) {
    return value == NULL ? 0 : (*env)->GetStringLength(env, value);
}

static jint inline_string_hashCode(JNIEnv *env, jstring value) {
    if (value == NULL) return 0;
    jint length = (*env)->GetStringLength(env, value);
    const jchar *characters = (*env)->GetStringChars(env, value, NULL);
    if (characters == NULL) return 0;
    jint hash = 0;
    for (jint index = 0; index < length; index++) hash = 31 * hash + characters[index];
    (*env)->ReleaseStringChars(env, value, characters);
    return hash;
}

static jchar inline_string_charAt(JNIEnv *env, jstring value, jint index) {
    if (value == NULL) {
        throw_npe(env, "String is null");
        return 0;
    }
    jint length = (*env)->GetStringLength(env, value);
    if (index < 0 || index >= length) {
        throw_aioobe(env, "String index out of range");
        return 0;
    }
    const jchar *characters = (*env)->GetStringChars(env, value, NULL);
    if (characters == NULL) return 0;
    jchar result = characters[index];
    (*env)->ReleaseStringChars(env, value, characters);
    return result;
}

static jclass inline_object_getClass(JNIEnv *env, jobject value) {
    return value == NULL ? NULL : (*env)->GetObjectClass(env, value);
}

static void inline_system_arraycopy(
        JNIEnv *env, jobject source, jint source_position,
        jobject target, jint target_position, jint length) {
    if (source == NULL || target == NULL) {
        throw_npe(env, "arraycopy: null array");
        return;
    }
    init_global_cache(env);
    (*env)->CallStaticVoidMethod(
        env, g_cls_System, g_mid_System_arraycopy,
        source, source_position, target, target_position, length);
}

static jdouble inline_math_abs_d(jdouble value) { return fabs(value); }
static jfloat inline_math_abs_f(jfloat value) { return fabsf(value); }
static jint inline_math_abs_i(jint value) {
    return value < 0 ? (jint)(0u - (uint32_t)value) : value;
}
static jlong inline_math_abs_l(jlong value) {
    return value < 0 ? (jlong)(0ULL - (uint64_t)value) : value;
}
static jdouble inline_math_max_d(jdouble left, jdouble right) {
    return isnan(left) || isnan(right) ? left + right : fmax(left, right);
}
static jdouble inline_math_min_d(jdouble left, jdouble right) {
    return isnan(left) || isnan(right) ? left + right : fmin(left, right);
}
static jint inline_math_max_i(jint left, jint right) { return left > right ? left : right; }
static jint inline_math_min_i(jint left, jint right) { return left < right ? left : right; }
static jdouble inline_math_sin(jdouble value) { return sin(value); }
static jdouble inline_math_cos(jdouble value) { return cos(value); }
static jdouble inline_math_tan(jdouble value) { return tan(value); }
static jdouble inline_math_sqrt(jdouble value) { return sqrt(value); }
static jdouble inline_math_pow(jdouble left, jdouble right) { return pow(left, right); }
static jdouble inline_math_log(jdouble value) { return log(value); }
static jdouble inline_math_exp(jdouble value) { return exp(value); }
static jdouble inline_math_floor(jdouble value) { return floor(value); }
static jdouble inline_math_ceil(jdouble value) { return ceil(value); }
static jlong inline_math_round(jdouble value) {
    if (isnan(value)) return 0;
    if (value >= 9223372036854775807.0) return INT64_MAX;
    if (value <= -9223372036854775808.0) return INT64_MIN;
    return (jlong)floor(value + 0.5);
}

static jint jnic_ishr(jint value, jint distance) {
    uint32_t shift = ((uint32_t)distance) & 31u;
    if (shift == 0) return value;
    uint32_t bits = (uint32_t)value;
    return value >= 0 ? (jint)(bits >> shift) : (jint)~((~bits) >> shift);
}
static jlong jnic_lshr(jlong value, jint distance) {
    uint32_t shift = ((uint32_t)distance) & 63u;
    if (shift == 0) return value;
    uint64_t bits = (uint64_t)value;
    return value >= 0 ? (jlong)(bits >> shift) : (jlong)~((~bits) >> shift);
}
static jint jnic_d2i(jdouble value) {
    if (isnan(value)) return 0;
    if (value >= 2147483647.0) return INT32_MAX;
    if (value <= -2147483648.0) return INT32_MIN;
    return (jint)value;
}
static jlong jnic_d2l(jdouble value) {
    if (isnan(value)) return 0;
    if (value >= 9223372036854775807.0) return INT64_MAX;
    if (value <= -9223372036854775808.0) return INT64_MIN;
    return (jlong)value;
}

static void throw_npe(JNIEnv *env, const char *message) {
    init_global_cache(env);
    jclass type = get_or_cache_class(
        env, &g_cls_NullPointerException, "java/lang/NullPointerException");
    if (type != NULL) (*env)->ThrowNew(env, type, message);
}
static void throw_aioobe(JNIEnv *env, const char *message) {
    init_global_cache(env);
    jclass type = get_or_cache_class(
        env, &g_cls_ArrayIndexOutOfBoundsException,
        "java/lang/ArrayIndexOutOfBoundsException");
    if (type != NULL) (*env)->ThrowNew(env, type, message);
}
static void throw_arith(JNIEnv *env, const char *message) {
    init_global_cache(env);
    jclass type = get_or_cache_class(
        env, &g_cls_ArithmeticException, "java/lang/ArithmeticException");
    if (type != NULL) (*env)->ThrowNew(env, type, message);
}
