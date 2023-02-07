# Dokkatoo

Generates documentation from Kotlin code.

Based on Kotlin Dokka.

* Compatible with Gradle Build Cache
* Compatible with Gradle Configuration Cache
* Safe cross-project sharing and aggregation

## Usage

### Applying the Gradle plugin

Dokkatoo is published to a GitHub branch which must be defined as a Gradle Plugin repository.

For example, using the Plugin Management DSL

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

The Dokkatoo plugin can be then be applied to any Gradle project.

```kts
// build.gradle.kts

plugins {
  id("dev.adamko.dokkatoo") version "0.0.1-SNAPSHOT"
}
```

Alternatively, the Maven Coordinates of the Dokkatoo plugin can be defined in an included build

```kts
// buildSrc/settings.gradle.kts

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://raw.githubusercontent.com/adamko-dev/dokkatoo/artifacts/m2/")
  }
}
```

Then the plugin can be applied in any Gradle project, without needing to define a version

```kts
// build.gradle.kts

plugins {
  id("dev.adamko.dokkatoo") // version defined in buildSrc/build.gradle.kts
}
```

#### Dokkatoo Gradle Tasks

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

### Specific formats

By default, the Dokkatoo plugin sets up publication of multiple formats: HTML,
GFM (GitHub Flavoured Markdown), Jekyll, and Javadoc. If you only want to generate one format, then
specific plugins are provided:

```kts
// ./build.gradle.kts

plugins {
  id("dev.adamko.dokkatoo-html")
  id("dev.adamko.dokkatoo-gfm")
  id("dev.adamko.dokkatoo-jekyll")
  id("dev.adamko.dokkatoo-javadoc")
}
```
