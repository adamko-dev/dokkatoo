# Dokkatoo Composite Build Example

This project demonstrates how to use Dokkatoo to aggregate modules across a
[composite build projects](https://docs.gradle.org/current/userguide/composite_builds.html).

> [!WARNING]
> HTML is the only format that correctly supports multimodule aggregation.
> This is a limitation of Dokka.

### Summary

There are 4 included builds.

* [`build-logic`](./build-logic) contains Kotlin/JVM and Dokkatoo convention plugins.
* [`module-kakapo`](./module-kakapo) and [`module-kea`](./module-kea)
  (named after [New Zealand birds](https://en.wikipedia.org/wiki/Birds_of_New_Zealand))
  represent regular Kotlin/JVM projects.
* [`docs`](./docs) aggregates the modules.

### Run locally

To run locally, follow these steps.

1. In the root Dokkatoo project directory, run `./gradlew assemble publishToTestMavenRepo`.
2. Either open the example project in an IDE, or `cd` into it.
3. In the example project, run `gradle build`.

The docs will be generated into [`./docs/build/dokka/`](./docs/build/dokka/).

## Distinct module paths

> [!IMPORTANT]
> When Dokkatoo aggregates modules, each module **must** have a distinct `modulePath`.
>
> When using composite builds, project paths may clash, so make sure to set a distinct `modulePath`.

The module path determines where each Dokka Module will be located within an aggregated
Dokka Publication.

By default, the module path is set to be the project path, which are distinct for a single
Gradle build. With composite builds the project paths may not be distinct, causing Dokkatoo
to overwrite modules.

This can be achieved in a convention plugin.
[build-logic/src/main/kotlin/dokka-convention.gradle.kts](./build-logic/src/main/kotlin/dokka-convention.gradle.kts). 
