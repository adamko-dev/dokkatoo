---
id: configuring
title: Configuring Dokkatoo
description: Overviews on how to configure Dokkatoo
sidebar_position: 4
slug: /configuring-dokkatoo/
---

# Configuring Dokkatoo

Once the Dokkatoo plugin is applied to a project, it can be configuring using the `dokkatoo {}` DSL.

Here is an example - it is not exhaustive and does not cover all functionality.

```kotlin title="build.gradle.kts"
import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters

plugins {
  id("dev.adamko.dokkatoo") version "$dokkatooVersion"
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

  pluginsConfiguration.html {
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

  // The default versions that Dokkatoo uses can be overridden:
  versions {
    jetbrainsDokka.set("1.9.20")
  }
}
```
