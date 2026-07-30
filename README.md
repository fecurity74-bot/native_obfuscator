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
