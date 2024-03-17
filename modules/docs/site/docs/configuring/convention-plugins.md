# Sharing Conventions

Often when creating Gradle Builds you'll want to
clean up the build scripts by organizing build logic so that common logic is isolated,
or de-duplicating some configuration.

Sharing build configuration is especially important in large projects,
both to ensure consistency and to make sure it doesn't grow out of control!
The best way to achieve this with Gradle is to use _convention plugins_.

This page gives a brief introduction to convention plugins.
Gradle can be an incredibly confusing and frustrating beast, but once you tame it,
its power is incredible, so let's get started.

## Summary

Here's a guide how to create the _absolute_ bare minimum for a convention plugin.

Let's say you have a basic Gradle project (without any subprojects).

```text
.
└── my-cool-project/
    ├── src/
    │   ├── main/kotlin/
    │   │   └── (sources)
    │   └── test/kotlin/
    │       └── (sources)
    ├── build.gradle.kts
    └── settings.gradle.kts
```

Here's the summary of what we'll achieve:

1. [Set up buildSrc](#setting-up-buildsrc),
   the helper-project that will contain our convention plugin.
2. [Create a convention plugin](#create-a-convention-plugin).
3. [Add Dokkatoo to the convention plugin](#add-dokkatoo-to-buildsrc-dependencies).
4. [Configure Dokkatoo, using the convention plugin](#apply-dokkatoo-in-the-convention-plugin).

What we'll end up with is a flexible, compartmentalized, and re-usable convention for configuring
Dokkatoo.

### Setting up buildSrc

First, let's create the build config
for [buildSrc](https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:build_sources).

Since buildSrc is effectively a standalone project, it's best to create a `settings.gradle.kts`
file,
and at the same time we can define the repositories Gradle will use with a
[Centralizing repositories declaration](https://docs.gradle.org/current/userguide/declaring_repositories.html#sub:centralized-repository-declaration).

```kotlin title="buildSrc/settings.gradle.kts"
rootProject.name = "buildSrc"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}
```

`buildSrc/build.gradle.kts` just needs
the [Kotlin DSL plugin](https://docs.gradle.org/current/userguide/kotlin_dsl.html#sec:kotlin-dsl_plugin).

This will make writing our convention plugin much easier, as we can use
a [precompiled script plugin](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins)

```kotlin title="buildSrc/build.gradle.kts"
plugins {
  `kotlin-dsl`
}
```

That's the minimum needed for buildSrc! Our project now looks like this:

```text
.
└── my-cool-project/
    ├── buildSrc/
    │   ├── build.gradle.kts
    │   └── settings.gradle.kts
    ├── src/
    │   ├── main/kotlin/
    │   │   └── (sources)
    │   └── test/kotlin/
    │       └── (sources)
    ├── build.gradle.kts
    └── settings.gradle.kts
```

### Create a convention plugin.

Let's create our convention plugin, that we will (eventually) use to share conventions.
But first, let's just make sometime simple. It's going to print a log message whenever it's
applied to a project.

Create the convention plugin for Dokkatoo:

```kotlin title="buildSrc/buildSrc/src/main/kotlin/my/conventions/dokkatoo.gradle.kts"
package my.conventions

logger.lifecycle("I don't do anything yet...")
```

Done!

What's nice about pre-compiled script plugins is that they can be used _just_ like regular
project `build.gradle.kts` files, so it's easy to jump between them.

:::info

The location and [`package`](https://en.wikipedia.org/wiki/Java_package) of pre-compiled
script plugins are important.

The name of the file (everything before `.gradle.kts`), and the `package` (if one is defined)
will be the plugin ID.

In our case, the plugin ID is `my.conventions.dokkatoo`.

:::

```text
.
└── my-cool-project/
    ├── buildSrc/
    │   ├── src/main/kotlin/
    │   │   └── my/conventions/
    │   │       └── dokkatoo.gradle.kts
    │   ├── build.gradle.kts
    │   └── settings.gradle.kts
    ├── src/
    │   ├── main/kotlin/
    │   │   └── (sources)
    │   └── test/kotlin/
    │       └── (sources)
    ├── build.gradle.kts
    └── settings.gradle.kts
```

You can test our progress so far by applying this do-nothing plugin to your root project's build
script.

```kotlin title="build.gradle.kts"
plugins {
  kotlin("jvm") version "1.9.23"
  id("my.conventions.dokkatoo")
}
```

And now, if you run `gradle assemble --quiet`, you'll see the plugin's message logged to the
console.

```text
I don't do anything yet...
```

### Add Dokkatoo to buildSrc dependencies

The plugin isn't useful at the moment. How can we make it configure Dokkatoo?

The first step is to add Dokkatoo as a buildSrc dependency.

```kotlin title="buildSrc/build.gradle.kts"
plugins {
  `kotlin-dsl`
}

dependencies {
  // add the *Maven coordinates* of the Dokkatoo, not the plugin ID, as a dependency
  // highlight-next-line
  implementation("dev.adamko.dokkatoo:dokkatoo-plugin:$dokkatooVersion")
}
```

:::tip
For the latest Dokkatoo version, check the [releases](/../releases) page.
:::

### Apply Dokkatoo in the Convention Plugin

The `dev.adamko.dokkatoo:dokkatoo-plugin` contains _all_ available Dokka output formats
(HTML, Javadoc, Jekyll, GFM), but you can choose which format to apply to your project as normal,
with the plugin ID.

For this example, let's apply the HTML plugin:

```kotlin title="buildSrc/buildSrc/src/main/kotlin/my/conventions/dokkatoo.gradle.kts"
package my.conventions

plugins {
  id("dev.adamko.dokkatoo-html")
}
```

:::note
Do not set a version when adding plugins to convention plugins. Gradle requires the version of
plugins is only set in a single location, and that place is the dependency in
`buildSrc/build.gradle.kts`.

If you try to apply Dokkatoo in buildSrc _and_ in a subproject, you might see this error:

```text
Error: Plugin request for plugin already on the classpath must not include a version
```

To resolve this, remove the version from all places where Dokkatoo is defined, *except* for
`buildSrc/build.gradle.kts`.
:::

Now, if you run `gradlew dokkatooGenerate`, you'll see that the `my.conventions.dokkatoo` plugin
has automatically applied Dokkatoo to the root project, and so the task runs.

### Configuring Dokkatoo conventions

Now we can use the convention plugin to define some conventional values.

For example, let's set a default HTML footer message.

```kotlin title="buildSrc/buildSrc/src/main/kotlin/my/conventions/dokkatoo.gradle.kts"
package my.conventions

plugins {
  id("dev.adamko.dokkatoo-html")
}

dokkatoo {
  pluginsConfiguration.html {
    // highlight-next-line
    footerMessage.convention("(C) My Cool Project")
  }
}
```

:::tip
When working with buildSrc convention plugins in an IDE, make sure to 'project sync' to make 
sure that your IDE refreshes.
:::


Now if you run `gradle dokkatooGenerate` again, you'll see the footer has changed.

### Overriding the conventions

What's great about convention plugins is that the values they set are just defaults, and they
can be overridden later.

For example, we could decide that we don't like the default footer, and so we can override it:

```kotlin title="build.gradle.kts"
plugins {
  kotlin("jvm") version "1.9.23"
  id("my.conventions.dokkatoo")
}

dokkatoo {
  pluginsConfiguration.html {
    // highlight-next-line
    footerMessage.set("COPYRIGHT 2020-2024 JAMIE LENMAN")
  }
}
```
