# Dokkatoo

Generates documentation from Kotlin code.

Based on Kotlin Dokka.

* Compatible with Gradle Build Cache
* Compatible with Gradle Configuration Cache
* Safe cross-project sharing and aggregation

## Usage

Dokkatoo is published to a GitHub branch which must be defined as a repository.

```kts
// settings.gradle.kts

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://raw.githubusercontent.com/adamko-dev/dokkatoo/artifacts/m2/")
  }
}
```

The plugin can be then be applied to any Gradle project.

```kts
// build.gradle.kts

plugins {
  id("dev.adamko.dokkatoo") version "0.0.1-SNAPSHOT"
}
```

Then any of the Dokkatoo tasks can be run to generate a documentation site in the
`build/dokkatoo` directory.

```shell
# generate all sites
./gradlew dokkatooGenerate

# only generate a single format
./gradlew dokkatooGenerateHtml
./gradlew dokkatooGenerateGfm
./gradlew dokkatooGenerateJekyll
./gradlew dokkatooGenerateJavadoc
```

### Combining subprojects

Any subproject can aggregate multiple subprojects into one Dokka Publication.

```kts
// ./build.gradle.kts

plugins {
  id("dev.adamko.dokkatoo") version "0.0.1-SNAPSHOT"
}

dependencies {
  // aggregate both subproject-hello and subproject-world
  // the subprojects must also have Dokkatoo applied
  dokkatoo(projects(":subproject-hello"))
  dokkatoo(projects(":subproject-world"))
}
```
