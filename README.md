[![GitHub license](https://img.shields.io/github/license/adamko-dev/dokkatoo?style=for-the-badge)](https://github.com/adamko-dev/dokkatoo/blob/main/LICENSE)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/dev.adamko.dokkatoo?style=for-the-badge)](https://plugins.gradle.org/search?term=dokkatoo)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v?label=MAVEN%20SNAPSHOT&metadataUrl=https%3A%2F%2Fraw.githubusercontent.com%2Fadamko-dev%2Fdokkatoo%2Fartifacts%2Fm2%2Fdev%2Fadamko%2Fdokkatoo%2Fdokkatoo-plugin%2Fmaven-metadata.xml&style=for-the-badge)](https://github.com/adamko-dev/dokkatoo/tree/artifacts#readme)

[![Dokkatoo Banner](docs/images/banner.svg)](https://github.com/adamko-dev/dokkatoo)

[Dokkatoo](https://github.com/adamko-dev/dokkatoo) is a Gradle plugin that generates documentation
for your Kotlin projects.

Under the hood it uses [Dokka](https://github.com/Kotlin/dokka/),
the API documentation engine for Kotlin.

###### Why Dokkatoo?

If
[Dokka already has a Gradle plugin](https://kotlinlang.org/docs/dokka-gradle.html),
then what is Dokkatoo for?

Dokkatoo has a number of improvements over the existing Dokka Gradle Plugin:

* Compatible with [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
* Compatible with
  [Gradle Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
* Safe cross-project sharing and aggregation
* Parallel execution

### Status

Dokkatoo has basic functionality, and can generate documentation for single-projects and
multimodule projects.

Be aware that many things are untested, broken, and undocumented. Please
[create an issue](https://github.com/adamko-dev/dokkatoo/issues)
if something is not as you'd expect, or like.

## Usage

Dokkatoo is published on
the [Gradle Plugin Portal](https://plugins.gradle.org/search?term=dokkatoo).

### Quick start

1. Apply the appropriate plugin for any formats you'd like to generate.

   For example, HTML and Javadoc
   ```kotlin
   // build.gradle.kts
   
   plugins {
     // only generate HTML and Javadoc
     id("dev.adamko.dokkatoo-html") version "$dokkatooVersion"
     id("dev.adamko.dokkatoo-javadoc") version "$dokkatooVersion"
     //id("dev.adamko.dokkatoo-gfm") version "$dokkatooVersion"
     //id("dev.adamko.dokkatoo-jekyll") version "$dokkatooVersion"
   }
   ```
   Or all formats

   ```kotlin
   // build.gradle.kts
  
   plugins {
     // generate all formats - HTML, Jekyll, Javadoc, and GFM (GitHub Flavoured Markdown)
     id("dev.adamko.dokkatoo") version "$dokkatooVersion"
   }
   ```
   [Read more about the available formats in the Dokka docs](https://github.com/Kotlin/dokka#output-formats).
2. Run the generation task

   ```shell
   ./gradlew dokkatooGenerate
   ```

3. View the results in `./build/dokka/`

#### Configuring Dokkatoo

Once the Dokkatoo plugin is applied to a project, it can be configuring using the `dokkatoo {}` DSL.

Here is an example - it is not exhaustive and does not cover all functionality.

```kotlin
// build.gradle.kts
import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters

plugins {
  id("dev.adamko.dokkatoo-html") version "$dokkatooVersion"
}

dokkatoo {
  moduleName.set("Basic Project")
  dokkatooSourceSets.configureEach {
    documentedVisibilities(
      VisibilityModifier.PUBLIC,
      VisibilityModifier.PROTECTED,
    )
    suppressedFiles.from(file("src/main/kotlin/it/suppressedByPath"))
    perPackageOption {
      matchingRegex.set("it.suppressedByPackage.*")
      suppress.set(true)
    }
    perPackageOption {
      matchingRegex.set("it.overriddenVisibility.*")
      documentedVisibilities(
        DokkaConfiguration.Visibility.PRIVATE
      )
    }
  }

  pluginsConfiguration.named<DokkaHtmlPluginParameters>("html") {
    customStyleSheets.from(
      "./customResources/logo-styles.css",
      "./customResources/custom-style-to-add.css",
    )
    customAssets.from(
      "./customResources/custom-resource.svg",
    )
    footerMessage.set("(C) The Owner")
  }
  dokkatooPublications.configureEach {
    suppressObviousFunctions.set(true)
    suppressObviousFunctions.set(false)
  }
}
```

#### Combining subprojects

Dokkatoo can aggregate documentation from subprojects.

To do this, apply the Dokkatoo plugin in all subprojects that should be documented.

In the aggregating project, depend on the other subprojects.

```kts
// build.gradle.kts

plugins {
  id("dev.adamko.dokkatoo-html") version "$dokkatooVersion"
}

dependencies {
  // aggregate both subproject-hello and subproject-world
  // the subprojects must also have Dokkatoo applied
  dokkatoo(projects(":subproject-hello"))
  dokkatoo(projects(":subproject-world"))
}
```

Run the Dokkatoo generation task.

```shell
./gradlew :dokkatooGeneratePublicationHtml
```

Only run the task in the aggregating project (prefix the task name with the subproject path)
so that Dokkatoo doesn't generate documentation in other subprojects (it won't cause problems, but
it will be slower.)

Dokkatoo will then generate documentation into `./docs/build/dokka/`

### Migrating from Dokka Gradle Plugin

Dokkatoo is not a drop-in replacement for the Dokka Gradle Plugin, and requires migration.

When Dokkatoo matures, a guide will be made available. For now, check the
[example projects](./examples#readme) for comparative examples.

###### Apply both Dokka Gradle Plugin and Dokkatoo

For help in migrating from the Dokka Gradle Plugin to Dokkatoo, you can still apply both plugins -
just make sure to update the Dokkatoo output directory!

```kotlin
// build.gradle.kts

plugins {
  id("org.jetbrains.dokka") version "$dokkaVersion"
  id("dev.adamko.dokkatoo-html") version "$dokkatooVersion"
}

dokkatoo {
  // update the output directory, so it doesn't clash with the Dokka plugin! 
  dokkatooPublicationDirectory.set(layout.buildDirectory.dir("dokkatoo"))
}
```

### Snapshot releases

Snapshot versions of Dokkatoo are available. They are published to a GitHub branch, which must be
added as a
[custom Gradle Plugin repository](https://docs.gradle.org/current/userguide/plugins.html#sec:custom_plugin_repositories)

```kts
// settings.gradle.kts

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()

    // add the Dokkatoo snapshot repository
    maven("https://raw.githubusercontent.com/adamko-dev/dokkatoo/artifacts/m2/") {
      name = "Dokkatoo Snapshots"
      // only include Dokkatoo snapshots
      mavenContent {
        includeGroup("dev.adamko.dokkatoo")
        snapshotsOnly()
      }
    }
  }
}
```
