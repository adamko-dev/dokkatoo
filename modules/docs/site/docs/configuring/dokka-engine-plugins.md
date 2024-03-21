# Dokka Engine Plugins

The generation that Dokka generator produces is completely determined
by the available Dokka Engine Plugins.
In fact, all formats created by Dokka Generator are Dokka Engine Plugins.

## Adding Dokka Engine Plugins

To add a Dokka Engine Plugin to Dokkatoo, add it as a dependency to all formats, or in the format you want to modify.

Any [Gradle dependency format](https://docs.gradle.org/8.6/userguide/declaring_dependencies.html)
is supported.

```kotlin title="build.gradle.kts"
plugins {
  id("dev.adamko.dokkatoo")
}

dependencies {
  // Add a plugin when generating all formats, using Maven coordinates,
  dokkatooPlugin("com.glureau:html-mermaid-dokka-plugin:0.6.0")

  // Add a Version Catalog entry to a specific format
  dokkatooPluginHtml(libs.dokkaPlugins.somePlugin)

  // or a local JAR file
  dokkatooPluginJavadoc(files("dokka-plugins/my-custom-plugin.jar"))

  // or another subproject
  dokkatooPluginGfm(project(":local-dokka-plugin"))
}
```

## Configuring Dokka Engine Plugins

### Built-in Dokka Engine Plugins

The built-in HTML and Versioning Dokka Engine Plugins can be configured via the Dokkatoo DSL.

```kotlin title="build.gradle.kts"
plugins {
  id("dev.adamko.dokkatoo")
}

dokkatoo {
  pluginsConfiguration.html {
    footerMessage = "custom footer message"
  }

  pluginsConfiguration.versioning {
    olderVersionsDir = layout.projectDirectory.dir("kdoc-archive")
  }
}
```

### Custom Dokka Engine Plugins

For non-built-in Dokka Engine Plugins, there is an _experimental_ DSL that allows for manually
configuring a custom Dokka Engine Plugin.

:::warning

Dynamically configuring a Dokka Engine Plugin is experimental and has not been widely tested.
[Feedback is encouraged](https://github.com/adamko-dev/dokkatoo/issues/new)!

:::

```kotlin title="build.gradle.kts"
plugins {
  id("dev.adamko.dokkatoo")
}

dependencies {
  dokkatooPlugin("foo.example:custom-dokka-plugin:1.2.3")
}

dokkatoo {
  pluginsConfiguration.register<DokkaPluginParametersBuilder>("id.of.CustomPlugin") {
    property("option1", true)
    files("inputFiles") {
      from("foo.txt")
    }
  }
}
```

:::note

Configuring a custom DSL is required by Gradle, so that all input files and properties can be
tracked. This makes Dokkatoo fast, because it can avoid tasks that haven't changed.

Further reading:
[Incremental Build](https://docs.gradle.org/current/userguide/incremental_build.html).

:::
