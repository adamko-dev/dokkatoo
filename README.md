[![GitHub license](https://img.shields.io/github/license/adamko-dev/dokkatoo?style=for-the-badge)](https://github.com/adamko-dev/dokkatoo/blob/main/LICENSE)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/dev.adamko.dokkatoo?style=for-the-badge&logo=gradle)](https://plugins.gradle.org/search?term=dokkatoo)
[![Maven Central](https://img.shields.io/maven-central/v/dev.adamko.dokkatoo/dokkatoo-plugin?style=for-the-badge&logo=apache-maven&color=6545e7&link=https%3A%2F%2Fsearch.maven.org%2Fsearch%3Fq%3Dg%3Adev.adamko.dokkatoo)](https://search.maven.org/search?q=g:dev.adamko.dokkatoo)
[![Maven Central Snapshots](https://img.shields.io/nexus/s/dev.adamko.dokkatoo/dokkatoo-plugin?label=MAVEN%20SNAPSHOT&server=https%3A%2F%2Fs01.oss.sonatype.org&style=for-the-badge&logo=apache-maven)](https://s01.oss.sonatype.org/content/repositories/snapshots/dev/adamko/dokkatoo/dokkatoo-plugin/)
[![Slack](https://img.shields.io/badge/slack-%23dokka-white.svg?&style=for-the-badge&logo=slack)](https://slack-chats.kotlinlang.org/c/dokka)

<picture>
  <img alt="Dokkatoo Logo" src="./modules/docs/site/static/img/banner.svg" style="margin: 1em">
</picture>

---

## Status Update

Dokkatoo has been merged into the main Dokka project!
In October 2025 [Dokka v2.1.0](https://github.com/Kotlin/dokka/releases/tag/v2.1.0) was released,
which means the official Dokka Gradle plugin has all the same features as Dokkatoo,
as well as improved performance, stability, and support.

* Compatible with [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html).
* Compatible with
  [Gradle Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html).
* Follows Gradle best practices for plugin development, for a more stable experience.
* Faster, parallel execution.
* K2 Analysis is enabled by default.
* Support
  for [Context parameters](https://kotlinlang.org/docs/whatsnew22.html#preview-of-context-parameters)
  and [Nested typealiases](https://kotlinlang.org/docs/whatsnew22.html#support-for-nested-type-aliases).

The Dokkatoo Gradle plugin is now obsolete. It will no longer be maintained.
It will be deprecated in a future release.

### Migration from Dokkatoo to Dokka Gradle plugin

Please migrate from Dokkatoo to Dokka to ensure you receive the latest features, bug fixes, and
support.

- Use the new plugin ID:
    - Replace `id("dev.adamko.dokkatoo")` with `id("org.jetbrains.dokka")`.
    - Replace `id("dev.adamko.dokkatoo-html")` with `id("org.jetbrains.dokka")`.
    - Replace `id("dev.adamko.dokkatoo-javadoc")` with `id("org.jetbrains.dokka-javadoc")`.

  _Note: The experimental Jekyll and GitHub Markdown formats are not available in Dokka 2.1.0._\
  _See [The future of Dokkatoo](#the-future-of-dokkatoo) below._
- Update any import statements. The packages are different, so the classes must be re-imported.
  For example:
    - `dev.adamko.dokkatoo.DokkatooExtension` is now `org.jetbrains.dokka.gradle.DokkaExtension`.
    - `dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier` is now
      `org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier`.
- Replace `dokkatoo {}` with `dokka {}` in Gradle build scripts.
- Update any dependencies:
    - Replace `dokkatoo(project(":some-subproject"))` with `dokka(project(":some-subproject"))`.
    - Replace `dokkatooPlugin(...)` with `dokkaPlugin(...)`.
- Update any task names from `dokkatoo` to `dokka`.

The official DGPv1 to DGPv2 docs might be helpful too:
https://kotl.in/dokka-gradle-migration

Migration might be a bit more complicated than that, so if anything is unclear please reach out in
[Kotlinlang Slack](https://kotl.in/slack) `#dokka`
or [create an issue](https://kotl.in/dokka-issues).

### The future of Dokkatoo

If Dokkatoo is obsolete, what is the purpose of this repository?

I think we can still use Dokkatoo as a force for good. Dokka has always been very pluggable,
and there is a scattering of useful plugins.
I want to make Dokkatoo a hub for community-maintained plugins for Dokka.

Since the Dokka team do not have the capacity to maintain the Jekyll and GitHub Markdown plugins,
instead we can migrate them into Dokkatoo, where they can be maintained in this repo,
and the community can contribute to them.

If you are interested in contributing a Dokka plugin, or maintaining an existing one,
please reach out in
[Kotlinlang Slack](https://kotl.in/slack) `#dokka`
or [create an issue](https://github.com/adamko-dev/dokkatoo/issues/new).

### Thanks

Lastly, I want to thank everyone who made Dokkatoo such a success.
I'm sure Dokkatoo was just another library for most people, but for me, it represents a milestone
of my life.
I started Dokkatoo as a hobby (or, more accurately,
a [yak-shaving](https://softwareengineering.stackexchange.com/q/388092/400690))
project. My efforts were noticed and encouraged, and JetBrains hired me to merge Dokkatoo into
Dokka.

To everyone in the Dokka, Kotlin Build Tools, and Kotlin teams: thanks for the help, support, and
code reviews.
And I especially want to thank all the users who gave feedback, reported bugs, requested features,
opened pull requests, or even just those who quietly used Dokkatoo.
I always appreciated your support.

When I look back on Dokkatoo, two things stand out. The first is the high point, which was
when [Gradle decided to use Dokkatoo to generate Gradle docs](https://github.com/gradle/gradle/pull/24302) -
I had written a plugin good enough for Gradle itself! The second is just how confusing the name was
in meetings. I laughed every time someone said "Dokkatoo" and someone else thought they said
"Dokka 2" - exactly what I wanted!

---

<details>

<summary>Click here to view the rest of the readme</summary>

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

</details>
