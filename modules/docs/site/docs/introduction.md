---
id: introduction
title: Introduction
description: A general overview of Dokkatoo, before diving into the details
sidebar_position: 1
slug: /
---

# Introduction to Dokkatoo

Welcome to the documentation website for Dokkatoo,

Dokkatoo is a Gradle plugin that generates documentation
for your Kotlin projects.

Dokkatoo uses [Dokka](https://github.com/Kotlin/dokka/), the API documentation engine for Kotlin,
to generate API reference documentation from source code.

###### Why Dokkatoo?

If
[Dokka already has a Gradle plugin](https://kotlinlang.org/docs/dokka-gradle.html),
then what is Dokkatoo for?

Dokkatoo has a number of improvements over the existing Dokka Gradle Plugin:

* Compatible with [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html).
* Compatible with
  [Gradle Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html).
* Follows Gradle best practices for plugin development, for a more stable experience.
* Faster, parallel execution, allows for cleaner build scripts and more fine-grained control.

### Status

Dokkatoo is used in production by many projects ([see the Showcase!](/showcase)), 
generating documentation for single-module and multimodule projects, in multiple different formats.

[Dokkatoo has been merged into the Dokka codebase](https://github.com/Kotlin/dokka/pull/3188),
although as of December 2023 it has not been released.
Until JetBrains releases a new Dokka Gradle Plugin,
please continue to use Dokkatoo, and
[watch this space...](https://github.com/Kotlin/dokka/issues/3131)
