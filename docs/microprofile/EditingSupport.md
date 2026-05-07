# MicroProfile editing support

## In properties files

Completion, validation, definition, hover in application.properties, microprofile-config.properties

![Micro Profile Config Support](../images/microprofile/MicroProfileConfigSupport.gif)

## In Java files

Validation, completion according MicroProfile extensions.

## Type Converters

MicroProfile Config uses converters to transform string values from configuration files into Java types. The IDE can display which converter is used for each property and validate values according to the converter's rules.

### Example: Duration Property

Consider this class that injects a `Duration` configuration property:

```java
package app;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.Duration;

public class SomeConfig {

    @ConfigProperty(name="some.time", defaultValue = "PT0.01S")
    private Duration someTime;
}
```

The `Duration` type requires a converter to parse string values like `"PT0.01S"` (ISO-8601 duration format) or `"10ms"` into Java `Duration` objects.

### Displaying Converter Information

The IDE can show which MicroProfile converter is used for each property using inlay hints.

#### Enable Converter Display

To see converter information inline:

1. Go to **Settings > Languages & Frameworks > MicroProfile > Properties**
2. Check **"Show MicroProfile converters of configuration properties"**

![Show MicroProfile Converters Settings](../images/microprofile/ShowMicroProfileConvertersSettings.png)

#### Converter Hints in Java Files

Once enabled, inlay hints appear next to `@ConfigProperty` fields showing the converter name. In this example, [StaticMethodConverter](https://github.com/smallrye/smallrye-config/blob/a3b49a863ed14664a82323ee66149bf5f223ef8f/implementation/src/main/java/io/smallrye/config/Converters.java#L1344) is displayed, indicating the converter used for the `Duration` type.

![Show MicroProfile Converters In Java File](../images/microprofile/ShowMicroProfileConvertersInJavaFile.png)

#### Converter Hints in Properties Files

The converter information also appears in `application.properties` files, prefixed with a colon (`:StaticMethodConverter`), helping you understand how values will be parsed.

![Show MicroProfile Converters In Properties File](../images/microprofile/ShowMicroProfileConvertersInPropertiesFile.png)

### Value Validation

The IDE validates configuration values according to the converter's parsing rules. Invalid values are highlighted with error markers.

#### Example: Invalid Duration Format

If you change the value to `"10ms"` (a format not supported by the default SmallRye converter), the IDE displays a validation error.

**In Java File (defaultValue):**

The error appears on the `defaultValue` attribute with the message: *"Text cannot be parsed to a Duration microprofile-config (DEFAULT_VALUE_IS_WRONG_TYPE)"*

![Validate Value In Java File](../images/microprofile/ValidateValueInJavaFile.png)

**In Properties File:**

The same validation error appears when using `10ms` in `application.properties`:

![Validate Value In Properties File](../images/microprofile/ValidateValueInPropertiesFile.png)

### Execution Mode: Safe vs Full

The validation behavior depends on the **execution mode** setting, which determines which converters are used for validation.

#### Safe Mode (Default)

By default, the IDE uses **safe mode**, which relies on an embedded SmallRye converter implementation. This mode:
- Works without running your application
- Uses standard MicroProfile converters
- May not support framework-specific extensions (e.g., Quarkus's `"10ms"` format)
- **Does not execute project code** - safe for untrusted projects

#### Full Mode

**Full mode** executes converters from your actual project classpath, including framework-specific implementations.

To switch to full mode:

1. Go to **Settings > Languages & Frameworks > MicroProfile**
2. Under **"Configure MicroProfile inspections"**, change **Execution mode** from `safe` to `full`

![Execution Mode Settings](../images/microprofile/ExecutionModeSettings.png)

> **⚠️ Security Warning**: Full mode executes converter code from your project's classpath. Only use full mode on projects you trust. A malicious converter could execute arbitrary code during validation. When working with untrusted code or projects from unknown sources, keep the execution mode on **safe**.

#### Full Mode Example: Quarkus DurationConverter

Quarkus provides its own [DurationConverter](https://github.com/quarkusio/quarkus/blob/main/core/runtime/src/main/java/io/quarkus/runtime/configuration/DurationConverter.java) that supports additional formats like `"10ms"`, `"5s"`, etc.

When you set execution mode to **full** in a Quarkus project:
- The IDE uses Quarkus's `DurationConverter` instead of the default SmallRye converter
- The value `"10ms"` is now valid and displays no error
- The inlay hint shows `DurationConverter` instead of `StaticMethodConverter`

![Validate Value In Java File with Full Mode](../images/microprofile/ValidateValueInJavaFileWithFullMode.png)

#### When to Use Each Mode

| Mode | Use When | Converters Used | Performance | Security |
|------|----------|----------------|-------------|----------|
| **Safe** | Standard MicroProfile projects or untrusted code | Embedded SmallRye | Fast (no project execution) | ✅ Safe |
| **Full** | Trusted projects with framework-specific formats | Your project's converters | Slower (requires classpath loading) | ⚠️ Executes project code |

**Recommendation**: Use **safe mode** for general development and when working with untrusted projects. Only switch to **full mode** for trusted projects when you need framework-specific converter validation.