# Dokka Engine Plugins

The generation that Dokka generator produces is completely determined
by the available Dokka Engine Plugins.
In fact, all formats created by Dokka Generator are Dokka Engine Plugins.

## Adding Dokka Engine Plugins

To add a Dokka Engine Plugin to Dokkatoo, add it as a dependency in the format you want to modify.

The
dependency  [Gradle dependency format](https://docs.gradle.org/8.6/userguide/declaring_dependencies.html)
is supported.

```kotlin title="build.gradle.kts"
plugins {
  id("dev.adamko.dokkatoo")
}

dependencies {
  // use Maven coordinates,
  dokkatooPluginHtml("com.glureau:html-mermaid-dokka-plugin:0.6.0")

  // or a Version Catalog plugin
  dokkatooPluginJekyll(libs.dokkaPlugins.somePlugin)

  // or a local file
  dokkatooPluginJavadoc(files("dokka-plugins/my-custom-plugin.jar"))

  // or a subproject
  dokkatooPluginGfm(project(":local-dokka-plugin"))
}
```

:::tip

For a shorter way of adding a Dokka Engine Plugin to _all_ Dokkatoo
formats, [watch this issue](https://github.com/adamko-dev/dokkatoo/issues/186).

:::

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
  dokkatooPluginHtml("foo.example:custom-dokka-plugin:1.2.3")
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
