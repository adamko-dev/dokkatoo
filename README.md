[![GitHub license](https://img.shields.io/github/license/adamko-dev/dokkatoo?style=for-the-badge)](https://github.com/adamko-dev/dokkatoo/blob/main/LICENSE)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/dev.adamko.dokkatoo?style=for-the-badge&logo=gradle)](https://plugins.gradle.org/search?term=dokkatoo)
[![Maven Central](https://img.shields.io/maven-central/v/dev.adamko.dokkatoo/dokkatoo-plugin?style=for-the-badge&logo=apache-maven&color=6545e7&link=https%3A%2F%2Fsearch.maven.org%2Fsearch%3Fq%3Dg%3Adev.adamko.dokkatoo)](https://search.maven.org/search?q=g:dev.adamko.dokkatoo)
[![Maven Central Snapshots](https://img.shields.io/maven-metadata/v?label=MAVEN%20SNAPSHOT&metadataUrl=https%3A%2F%2Fs01.oss.sonatype.org%2Fcontent%2Frepositories%2Fsnapshots%2Fdev%2Fadamko%2Fdokkatoo%2Fdokkatoo-plugin%2Fmaven-metadata.xml&style=for-the-badge&logo=apache-maven)](https://s01.oss.sonatype.org/content/repositories/snapshots/dev/adamko/dokkatoo/dokkatoo-plugin/)
[![Slack](https://img.shields.io/badge/slack-%23dokka-white.svg?&style=for-the-badge&logo=slack)](https://slack-chats.kotlinlang.org/c/dokka)

<picture>
  <img alt="Dokkatoo Logo" src="./modules/docs/site/static/img/banner.svg" style="margin: 1em">
</picture>

[Dokkatoo](https://github.com/adamko-dev/dokkatoo) is a
[Gradle](https://gradle.org/)
plugin that generates easy-to-use reference documentation for your
[Kotlin](https://kotlinlang.org/) (or Java!) projects.

## [For the full documentation, click here](https://adamko-dev.github.io/dokkatoo/)

## What can Dokkatoo do?

* **Automatic documentation** - Automatically generates up-to-date docs from your code, for both
  Kotlin and Java projects.
* **Format Flexibility** - Supports generating HTML, Javadoc, and Markdown output formats.
* **Customization King** - Make your documentation truly yours. With Dokkatoo, you can customize the
  output, including custom stylesheets and assets.
* **Gradle's Best Friend** - Compatible with _all_ of Gradle's most powerful features!
  Incremental compilation, multimodule builds, composite builds, Build Cache, Configuration Cache.

Under the hood Dokkatoo uses [Dokka](https://github.com/Kotlin/dokka/),
the API documentation engine for Kotlin.

## Showcase

For real-life examples of the documentation that Dokkatoo generates,
**check out** [**the showcase**](https://adamko-dev.github.io/dokkatoo/showcase)!

## Getting Started

View the
[documentation](https://adamko-dev.github.io/dokkatoo/)
for more detailed instructions about how to set up and use Dokkatoo.

### Quick start

To quickly generate documentation for your project, follow these steps.

> [!TIP]
> Dokkatoo supports multiple formats, but HTML is the quickest and easiest to get started with.

1. Check the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/dev.adamko.dokkatoo-html)
   to find the latest version of Dokkatoo.
2. Add the Dokkatoo plugin to your subproject:

   ```kotlin
   // build.gradle.kts

   plugins {
     kotlin("jvm")
     id("dev.adamko.dokkatoo-html")
   }
   ```

3. **(Optional)** If you'd like to combine multiple subprojects, add the Dokkatoo plugin to each
   subproject, and
   aggregate them in a single project by declaring dependencies to the subprojects.

   ```kotlin
   // build.gradle.kts
   plugins {
      id("dev.adamko.dokkatoo-html")
   }
   
   dependencies {
     // Aggregate both subproject-hello and subproject-world into the current subproject.
     // These subprojects must also have Dokkatoo applied.
     dokkatoo(project(":subproject-hello"))
     dokkatoo(project(":subproject-world"))
   }
   ```

4. Run the generation task:

   ```shell
   ./gradlew :dokkatooGenerate
   ```

5. View the results in `./build/dokka/`

For more detailed instructions about how to set up and use Dokkatoo, and control the output,
[more guides are available in the docs](https://adamko-dev.github.io/dokkatoo/docs).

## Releases

Dokkatoo is available from the
[Gradle Plugin Portal](https://plugins.gradle.org/search?term=dokkatoo)
and
[Maven Central](https://search.maven.org/search?q=g:dev.adamko.dokkatoo).
[Snapshot releases](https://adamko-dev.github.io/dokkatoo/docs/releases#snapshots)
are also available.

More details about the Dokkatoo releases is available in the documentation
[Dokkatoo Documentation](https://adamko-dev.github.io/dokkatoo/docs/releases)

## Why not Dokka?

If
[Dokka already has a Gradle plugin](https://kotlinlang.org/docs/dokka-gradle.html),
then what is Dokkatoo for?

Dokkatoo has a number of improvements over the existing Dokka Gradle Plugin:

* Compatible with [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html).
* Compatible with
  [Gradle Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html).
* Follows Gradle best practices for plugin development, for a more stable experience.
* Faster, parallel execution.

### Migrating from Dokka Gradle Plugin

Migrating from Dokka to Dokkatoo can be done in a few simple steps.
Check the [Dokkatoo Documentation](https://adamko-dev.github.io/dokkatoo/)
to get started.

If you'd like to see comparative examples of the same projects with both Dokka and Dokkatoo config,
check the [example projects](./examples/README.md).
