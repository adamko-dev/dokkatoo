---
id: releases
title: Releases
description: Where Dokkatoo is released
sidebar_position: 2
---

# Dokkatoo Releases

Dokkatoo is published on:

* [Gradle Plugin Portal](https://plugins.gradle.org/search?term=dokkatoo)
* [Maven Central](https://search.maven.org/search?q=g:dev.adamko.dokkatoo)

### Release notes

For notes about the latest changes and fixes are available
[GitHub Releases](https://github.com/adamko-dev/dokkatoo/releases).

### Snapshot releases

Snapshot versions of Dokkatoo are available on
[Maven Central](https://s01.oss.sonatype.org/content/repositories/snapshots/dev/adamko/dokkatoo/dokkatoo-plugin/).

```kotlin title="settings.gradle.kts"
pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()

    // add Maven Central snapshot repository
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
      name = "MavenCentralSnapshots"
      mavenContent { snapshotsOnly() }
    }
  }
}
```
