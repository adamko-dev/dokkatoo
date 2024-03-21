# Java Example project

This project demonstrates how Dokkatoo can be applied to a pure Java project
to generate documentation.

### Demonstration

To generate HTML documentation, run

```shell
gradle :dokkatooGeneratePublicationHtml
```

### Implementation details

Note that the `org.jetbrains.dokka:kotlin-as-java-plugin` Dokka Plugin
must be applied for Java sources to be rendered as Java.
(Despite the plugin's name, it also affects how Java sources are rendered.)

This example applies the `org.jetbrains.dokka:kotlin-as-java-plugin` Dokka Plugin
in the convention
[`./buildSrc/src/main/kotlin/dokka-conventions.gradle.kts` convention plugin](./buildSrc/src/main/kotlin/dokka-convention.gradle.kts).
