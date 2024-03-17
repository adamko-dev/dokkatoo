# Migrating from Dokka to Dokkatoo

If you have a project that already uses Dokka, then why migrate to Dokkatoo?

Here are some reasons, all of which sparked the reason to create Dokkatoo.

* Dokkatoo follows Gradle best practices.
* Dokkatoo is more performant, being compatible with
  [Incremental Build](https://docs.gradle.org/current/userguide/incremental_build.html),
  [Build Cache](https://docs.gradle.org/current/userguide/build_cache.html),
  and
  [Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html).
* Dokkatoo's configuration is easier, using a descriptive top level DSL (no more manual task
  configuration).
* Aggregating projects is simpler, as it relies on Gradle's dependency management.

### Applying both Dokka and Dokkatoo

The first step migrating from the official Dokka Gradle Plugin to Dokkatoo is easy: apply both
plugins.

Just make sure to update the Dokkatoo output directory!

<!-- @formatter:off -->
```kotlin title="build.gradle.kts"
plugins {
  id("org.jetbrains.dokka") version "$dokkaVersion"
  id("dev.adamko.dokkatoo") version "$dokkatooVersion"
}

dokkatoo {
  // update the output directory, so it doesn't clash with the Dokka plugin!
  // highlight-next-line
  dokkatooPublicationDirectory.set(layout.buildDirectory.dir("dokkatoo"))
}
```
<!-- @formatter:on -->


You can then do a comparative example on the output in both directories.

### Migrating Configuration

The next step can be more complicated: converting the Dokka configuration to Dokkatoo.

For examples of how to do this, see [examples in the Showcase](/showcase?tags=example).
Each Dokkatoo example projects has an equivalent Dokka project in the same directory.
