---
id: introduction
title: Introduction
description: A general overview of Dokkatoo, before diving into the details
sidebar_position: 1
slug: /
---

# Introduction to Dokkatoo

Welcome to the documentation website for Dokkatoo,

[Dokkatoo](https://github.com/adamko-dev/dokkatoo) is a
[Gradle](https://gradle.org/)
plugin that generates easy-to-use reference documentation for your
[Kotlin](https://kotlinlang.org/) (or Java!) projects.

## What can Dokkatoo do?

* **Automatic documentation** - Automatically generates up-to-date docs from your code, for both
  Kotlin and Java projects.
* **Format Flexibility** - Supports generating HTML, Javadoc, and Markdown output formats.
* **Customization King** - Make your documentation truly yours. With Dokkatoo, you can customize the
  output, including custom stylesheets and assets.
* **Gradle's Best Friend** - Compatible with _all_ of Gradle's most powerful features!
  Incremental compilation, multimodule builds, composite builds, Build Cache, Configuration Cache.

Dokkatoo uses [Dokka](https://github.com/Kotlin/dokka/), the API documentation engine for Kotlin,
to generate API reference documentation from source code.

## Why Dokkatoo?

If
[Dokka already has a Gradle plugin](https://kotlinlang.org/docs/dokka-gradle.html),
then what is Dokkatoo for?

Dokkatoo has a number of improvements over the existing Dokka Gradle Plugin:

* Easier to configure, with an easy-to-use build script DSL.
* Faster, parallel execution, with smart incremental compilation.
* Compatible with [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html).
* Compatible
  with [Gradle Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html).
* Follows Gradle best practices for plugin development, for a more stable experience.

## Status

Dokkatoo is used in production by many projects ([see the Showcase!](/showcase)),
generating documentation for single-module and multimodule projects, in multiple different formats.

[Dokkatoo has been merged into the Dokka codebase](https://github.com/Kotlin/dokka/pull/3188),
although as of December 2023 it has not been released.
Until JetBrains releases a new Dokka Gradle Plugin,
please continue to use Dokkatoo, and
[watch this space...](https://github.com/Kotlin/dokka/issues/3131)
