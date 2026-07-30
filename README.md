# Native Obfuscator

JVM-to-native obfuscator for Windows. Selected Java methods are translated to
native code, compiled with Zig and packaged into the output JAR as `j2c.dll`.

## Requirements

- JDK 17 or newer
- Zig available as `zig` in `PATH`, or `zig.exe` placed in the project directory

The local Zig executable and an unpacked Zig distribution are intentionally
excluded from Git.

## Build

```powershell
.\gradlew.bat shadowJar
```

The executable JAR is generated at `build/libs/jnic.jar`.

## Configuration

Edit `config.yml`:

```yaml
input: ./input.jar
output: ./output.jar
libs:
  - ./libs
annotationMode: true
includes:
  -
excludes:
  -
obfuscation:
  stringEncryption: false
  flowObfuscation: false
  antiDebug: true
```

When `annotationMode` is enabled, only methods annotated with
`@org.nativeobfuscator.annotation.Include` are processed:

```java
import org.nativeobfuscator.annotation.Include;

public class Example {
    @Include
    public static int add(int left, int right) {
        return left + right;
    }
}
```

Run the obfuscator from the project directory:

```powershell
java -jar build\libs\jnic.jar
```

The generated Windows library is stored inside the output JAR as `j2c.dll` and
loaded by `org.nativeobfuscator.NativeLoader`.

## Native generation pipeline

Native translation is split into independent stages:

1. ASM bytecode analysis and instruction dispatch.
2. Construction of a compact method IR retaining source indexes and opcodes.
3. IR normalization and conservative peephole optimization.
4. Class-lookup indirection and opaque control-flow insertion.
5. Lowering through C templates stored under `src/main/resources/native/templates`.
6. Manual JNI registration from `JNI_OnLoad`.

The compiler uses `-O3` for the reusable runtime and registration code.
Generated native method bodies alone are marked with `JNIC_NO_OPT`, preventing
LLVM from removing their intentionally complicated control flow.

Additional configuration:

```yaml
obfuscation:
  ir: true
  lifterResistance: true
optimization:
  ir: true
```

The lifter-resistance pass never changes Java semantics. Literal `FindClass`
operations are represented by encrypted lookup data and decoy metadata, while
the runtime helper always resolves the original requested class.
