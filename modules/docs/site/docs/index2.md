# Dokkatoo Documentation

Welcome to the documentation website for Dokkatoo,

Dokkatoo is a Gradle plugin that generates documentation
for your Kotlin projects.

Dokkatoo uses [Dokka](https://github.com/Kotlin/dokka/), the API documentation engine for Kotlin,
to generate API reference documentation from source code.

###### Why Dokkatoo?

If
[Dokka already has a Gradle plugin](https://kotlinlang.org/docs/dokka-gradle.html),
then what is Dokkatoo for?

Dokkatoo has a number of improvements over the existing Dokka Gradle Plugin:

* Compatible with [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
* Compatible with
  [Gradle Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
* Follows Gradle best practices for plugin development, for a more stable experience
* Faster, parallel execution


### Status

Dokkatoo is used in production by many projects, and can generate documentation for single-module
and multimodule projects.

[Dokkatoo has been merged into the Dokka codebase](https://github.com/Kotlin/dokka/pull/3188),
although as of December 2023 it has not been released.
Until JetBrains releases a version of Dokkatoo, continue to use this version and
[watch this space](https://github.com/Kotlin/dokka/issues/3131).

#### Snapshot releases

Snapshot versions of Dokkatoo are available on
[Maven Central](https://s01.oss.sonatype.org/content/repositories/snapshots/dev/adamko/dokkatoo/dokkatoo-plugin/).

```kotlin title="settings.gradle.kts"
pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()

    // add Maven Central snapshot repository
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
      name = "MavenCentralSnapshots"
      mavenContent { snapshotsOnly() }
    }
  }
}
```


## Usage

Dokkatoo is published on the
[Gradle Plugin Portal](https://plugins.gradle.org/search?term=dokkatoo)
and
[Maven Central](https://search.maven.org/search?q=g:dev.adamko.dokkatoo).
[Snapshot releases](#snapshot-releases) are also available.
