# Sharing Conventions

If you have a multimodule 

Gradle can be a incredibly confusing and frustrating beast, but once you tame it, 
its power is incredible. So let's get started.

## Summary

Here's a guide how to create the _absolute_ bare minimum for a convention plugin.

Let's say you have a basic Gradle project (without any subprojects).

```text
.
└── my-cool-project/
    ├── src/
    │   ├── main/
    │   │   └── kotlin/
    │   │       └── (sources)
    │   └── test/
    │       └── kotlin/
    │           └── (sources)
    ├── build.gradle.kts
    └── settings.gradle.kts
```

Here's the summary of what we'll achieve:

1. Set up
   [buildSrc](https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:build_sources),
   the helper-project that will contain out convention plugin. 
2. Create a convention plugin.



### Setting up buildSrc

First, let's create the build config for [buildSrc](https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:build_sources).

Since buildSrc is effectively a standalone project, it's best to create a `settings.gradle.kts` file,
and at the same time we can define the repositories gradle will use with a
[centralised repositories declaration](https://docs.gradle.org/current/userguide/declaring_repositories.html#sub:centralized-repository-declaration).

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

`buildSrc/build.gradle.kts` just needs the [Kotlin DSL plugin](https://docs.gradle.org/current/userguide/kotlin_dsl.html#sec:kotlin-dsl_plugin).

This will make writing our convention plugin much easier, as we can use a [precompiled script plugin](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins)

```kotlin title="buildSrc/build.gradle.kts"
plugins {
  `kotlin-dsl`
}
```

That's the minimum needed for buildSrc!

### Create a convention plugin.

Let's create our convention plugin, that we'll (eventually) use to share conventions.
But first, let's just make sometime simple.

Create the convention plugin for Dokkatoo:

```kotlin title="buildSrc/buildSrc/src/main/kotlin/my/conventions/dokkatoo.gradle.kts"
package my.conventions

logger.lifecycle("I don't do anything yet...")
```

Done!

:::tip

The name of the file (everything before `.gradle.kts`), and the [`package`](https://en.wikipedia.org/wiki/Java_package) (if one is defined) will be the plugin ID

:::


You can test it by applying this do-nothing plugin to your project.

```kotlin title="buildSrc/build.gradle.kts"
plugins {
  kotlin("jvm") version "1.9.23"
  id("my.conventions.dokkatoo")
}
```


And now, if you run `gradle help --quiet`, you'll see our message printed to the console.

```text
I don't do anything yet...
```


### Adding Dokkatoo



#### Add Dokkatoo to buildSrc dependencies

#### Apply Dokkatoo in 

---


### Setting up buildSrc

First, set-up

by creating a file `./buildSrc/build.gradle.kts`

In it, add the
[`kotlin-dsl` plugin](https://docs.gradle.org/current/userguide/kotlin_dsl.html#sec:kotlin-dsl_plugin),
and add the BCV-MU plugin as a dependency.

```kotlin title="build.gradle.kts"
plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  // add the *Maven coordinates* of the bcv-MU plugin, not the plugin ID, as a dependency
  implementation("dev.adamko.kotlin.binary-compatibility-validator:bcv-gradle-plugin:$bcvMuVersion")
}
```



:::note

If you try to apply Dokkatoo in your subproject _and_ define Dokkatoo in `buildSrc/build.gradle.kts`,
you might see this error:
```
Error: Plugin request for plugin already on the classpath must not include a version
```

What happens when you apply the above steps and you get an error Plugin request for plugin already on the classpath must not include a version?

There are a lot of ways to specify the version of a Gradle plugin (far too many in my opinion!). If you're updating an existing project with the above steps, you've just used one more way of specifying a version, and Gradle is spitting out an unhelpful error message.

The way to fix it is to limit the number of ways your project specifies Gradle plugin versions. What's probably happened is that you've added a plugin in buildSrc/build.gradle.kts that you also apply in the plugins block of a subproject. Gradle will get confused about which version of the plugin you want.

The way to fix it is to only specify the plugin version in a single place, and that place is as a dependency in buildSrc/build.gradle.kts.


:::


##### Shared configuration with a convention plugin

To share common configuration it is best to set up a convention-plugin.

If you don't have any convention plugins already, then here's a quick guide.

Next, create a convention plugin. This is where any shared configuration can be defined.

```kotlin
// buildSrc/src/main/kotlin/binary-compatibility-validator-convention.gradle.kts

plugins {
  id("dev.adamko.kotlin.binary-compatibility-validator") // no version needed - it's defined in buildSrc/build.gradle.kts
}

binaryCompatibilityValidator {
  ignoredClasses.add("com.company.BuildConfig")
}
```

Finally, apply the convention plugin to subprojects. This will automatically re-use the same
configuration.

```kotlin
// ./some/subproject/build.gradle.kts
plugins {
  id("binary-compatibility-validator-convention")
  kotlin("jvm")
}
```


Welcome to the fun world of [sharing outputs between Gradle subprojects](https://docs.gradle.org/current/userguide/cross_project_publications.html)!

There's a few different ways of solving this problem.

Since you're working with a JVM project you might want to configure a [feature variant](https://docs.gradle.org/current/userguide/feature_variants.html). Gradle will then create a new source set, and you can generate your files into this.


But, if you're not using a JVM project, then things get a little bit more complicated. I'll explain the simplest and most straight forward way. Please stop laughing, that wasn't a joke. Yes I know, it's still complicated. Trust me, it's worth it.

What we'll end up with is a robust way to share files between projects in a way that's cacheable (being compatible with both [Build Cache](https://docs.gradle.org/current/userguide/build_cache.html) and [Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)), flexible, re-usable, and gain a good understanding of how Gradle works.


### Tasks

Here's the summary of what needs to be done:

* Create a [buildSrc](https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:build_sources) convention plugin
* Set up some configurations\* for [_providing_ and _consuming_](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:resolvable-consumable-configs) files
* Define a custom [variant attribute](https://docs.gradle.org/current/userguide/variant_attributes.html) as a marker, to differentiate our configurations from others
* Put the files produced by a task into the _outgoing_ configuration
* Resolve the files from other subprojects using the _incoming_ configuration

_\*Yes, the name [configuration](https://docs.gradle.org/current/userguide/dependency_management_terminology.html#sub:terminology_configuration) is confusing. In this context 'configuration' should be better renamed as 'DependencyContainer' - they're just a collection of files that might be _outgoing_ or _incoming_ to a subproject, along with some metadata to describe the contents._

### Creating a buildSrc convention plugin

We need to be able to set-up our providing and consuming code in both the providing and consuming subprojects. While technically we could copy-paste it, that kind sucks and far too finickity. The best way to share config in Gradle is with a convention plugin.

I've gone over setting up convention plugins in another answer (https://stackoverflow.com/questions/71883613/configure-kotlin-extension-for-gradle-subprojects/71892685#71892685), so I'll just summarise the steps here.


When you run a task (e.g. `./gradlew help`), you should see the message `I don't do anything yet...` logged to the console.

### Creating configurations for providing and consuming files

The next step is creating some configurations, which are the [shipping containers](https://en.wikipedia.org/wiki/Shipping_container) of Gradle's dependencies world.

We're going to make two configurations, one for _incoming_ files, and another for _outgoing_ files.


Take note of `isCanBeConsumed` and `isCanBeResolved`\*. In Gradle terminology, the incoming one will be _resolved_ by a subproject, and the outgoing one will be _consumed_ by other subprojects. [It's important to use the right combination](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:resolvable-consumable-configs).

_\*Again, we have some confusing names. The terms 'consumed' and 'resolved' aren't very clear, they both seem like synonyms to me. They would be better renamed to indicate that `consumed=true && resolved=false` means `OUTGOING`, and `consumed=false && resolved=true` means `INCOMING`_


```kotlin
// buildSrc/src/main/kotlin/generated-files-sharing.gradle.kts

// register the incoming configuration
val generatedFiles by configurations.registering {
  description = "consumes generated files from other subprojects"

  // the dependencies in this configuration will be resolved by this subproject...
  isCanBeResolved = true
  // and so they won't be consumed by other subprojects
  isCanBeConsumed = false
}

// register the outgoing configuration
val generatedFilesProvider by configurations.registering {
  description = "provides generated files to other subprojects"
  
  // the dependencies in this configuration won't be resolved by this subproject...
  isCanBeResolved = false  
  // but they will be consumed by other subprojects
  isCanBeConsumed = true
}
```


What's cool about this step is that if you now jump to your subproject you can use these configurations as if they were built into Gradle (after an [IDE sync](https://www.jetbrains.com/idea/guide/tutorials/working-with-gradle/syncing-and-reloading/), if necessary).

```kotlin
// my-consumer/build.gradle.kts

plugins {
  id("generated-files-sharing")
}

dependencies {
  generatedFiles(project(":my-generator"))
}
```

Gradle has generated a [type-safe accessor](https://docs.gradle.org/current/userguide/kotlin_dsl.html#type-safe-accessors) for the `generatedFiles` configuration.

However, we're not done. We haven't put any files into the outgoing configuration, so of course the consuming project isn't going to be able to resolve any files. But before we do that, we need to add that metadata I mentioned.

### Differentiate our configurations with variant attributes

It's quite likely that the subproject that provides the generated files also has other configurations with completely different file types. But we don't want to fill up the `generatedFiles` configuration with lots of files, we _only_ want generated files produced by our generate task.

That's where [variant attributes](https://docs.gradle.org/current/userguide/dependency_management_terminology.html#sub:terminology_attribute) come in. If configurations were shipping containers, then the variant attributes are the shipping labels on the outside that describe where the contents should be sent.

At a basic level, variant attributes are just key-value strings, except the keys have to be [registered with Gradle](https://docs.gradle.org/current/userguide/variant_attributes.html#creating_attributes_in_a_build_script_or_plugin). We can skip that registration though if we use a built-in [standard attribute](https://docs.gradle.org/current/userguide/variant_attributes.html#sec:standard_attributes) that we can use. The [Usage](https://docs.gradle.org/current/javadoc/org/gradle/api/attributes/Usage.html) attribute is a good pick. It's pretty commonly used, so long as we pick a distinctive value Gradle is going to be able to differentiate between two configurations by comparing the values.



```kotlin
// buildSrc/src/main/kotlin/generated-files-sharing.gradle.kts

// create a custom Usage attribute value, with a distinctive value
val generatedFilesUsageAttribute: Usage =
  objects.named<Usage>("my.library.generated-files")

val generatedFiles by configurations.registering {
  description = "consumes generated files from other subprojects"

  isCanBeResolved = true
  isCanBeConsumed = false
  
  // add the attribute to the incoming configuration
  attributes {
    attribute(Usage.USAGE_ATTRIBUTE, generatedFilesUsageAttribute)
  }
}

val generatedFilesProvider by configurations.registering {
  description = "provides generated files to other subprojects"

  isCanBeResolved = false
  isCanBeConsumed = true

  // also add the attribute to the outgoing configuration
  attributes {
    attribute(Usage.USAGE_ATTRIBUTE, generatedFilesUsageAttribute)
  }
}
```

What's important is that the _same_ attribute key and value are added to both configurations. Now, Gradle can happily match both the incoming and outgoing configurations!

Now all the parts are in place. We're almost done! It's time to start pushing files into the configuration, and pulling files out.

### Putting files into the _outgoing_ configuration

In the subproject that produces the generated files, let's say we have some task that produces some files. I'll use a [Sync task](https://docs.gradle.org/current/userguide/working_with_files.html#sec:sync_task) as a stand-in for the actual generator task.

```kotlin
// my-generator/build.gradle.kts

plugins {
  id("generated-files-sharing")
}

val myGeneratorTask by tasks.registering(Sync::class) {
  from(resources.text.fromString("hello, world!"))
  into(temporaryDir)
}
```

Note that the output directory doesn't really matter, because thanks to Gradle's Provider API, a [task can be converted into a file-provider](https://docs.gradle.org/current/userguide/lazy_configuration.html#working_with_task_dependencies_in_lazy_properties).

```kotlin
// my-generator/build.gradle.kts

configurations.generatedFilesProvider.configure { 
  outgoing { 
    artifact(myGeneratorTask.map { it.temporaryDir })
  }
}
```

What's nice about this is that now Gradle will _only_ configure and run the `myGeneratorTask` task if it requested. When this kind of [configuration avoidance](https://docs.gradle.org/current/userguide/task_configuration_avoidance.html) is regularly used, then it can really help speed up Gradle builds.


### Resolving the incoming configuration

We're at the last step!

In the consuming project we can add a dependency on the providing project using the regular `dependencies {}` block.

```kotlin
// my-consumer/build.gradle.kts

plugins {
  id("generated-files-sharing")
}

dependencies {
  generatedFiles(project(":my-generator"))
}
```

And now we can get the _incoming_ files from the incoming configuration:

```kotlin
// my-consumer/build.gradle.kts

val myConsumerTask by tasks.registering(Sync::class) {
  from(configurations.generatedFiles.map { it.incoming.artifacts.artifactFiles })
  into(temporaryDir)
}
```

Now if you run `./gradlew myConsumerTask` you'll notice that, even though you haven't explicitly set any [task dependencies](https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:adding_dependencies_to_tasks), Gradle will automatically run `myGeneratorTask`.

And if you check the contents of `myConsumerTask`'s temporary directory (`./my-consumer/build/tmp/myConsumerTask/`), you'll see the generated file.

If you re-run the same command, then you should see that Gradle will avoid running the tasks because they are [UP-TO-DATE](https://stackoverflow.com/q/15137271/4161471).


You can also enable [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html) (`./gradlew myConsumerTask --build-cache`) and even if you delete generated files (remove  the `./my-generator/build/` directory) you should see that both `myGeneratorTask` and `myConsumerTask` are [loaded FROM-CACHE](https://docs.gradle.org/current/userguide/build_cache.html#sec:task_output_caching)
