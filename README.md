# Dokkatoo

Generates documentation from Kotlin code.

Based on Kotlin Dokka.

* Compatible with Gradle Build Cache
* Compatible with Gradle Configuration Cache
* Safe cross-project sharing and aggregation

## Usage

```kts
// build.gradle.kts

plugins {
  id("dev.adamko.dokkatoo") version "0.0.1-SNAPSHOT"
}

dokkatoo {
  // ...
}
```

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
  kotlin("jvm") version "1.8.0" apply false // required by Gradle
  id("dev.adamko.dokkatoo") version "0.0.1-SNAPSHOT"
}

dependencies {
  // aggregate both subproject-hello and subproject-world
  // the subprojects must also have Dokkatoo applied
  dokkatoo(projects(":subproject-hello"))
  dokkatoo(projects(":subproject-world"))
}
```
