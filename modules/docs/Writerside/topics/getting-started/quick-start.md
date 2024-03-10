# Quick Start

So, you have a Kotlin or Java project that uses Gradle and you want to generate some pretty
API reference documentation. You've come to the right place.

### Single Project

1. Apply the appropriate plugin for any formats you'd like to generate.

   For example, HTML and Javadoc
   ```kotlin
   // build.gradle.kts
   
   plugins {
     // only generate HTML and Javadoc
     id("dev.adamko.dokkatoo-html") version "$dokkatooVersion"
     id("dev.adamko.dokkatoo-javadoc") version "$dokkatooVersion"
     //id("dev.adamko.dokkatoo-gfm") version "$dokkatooVersion"
     //id("dev.adamko.dokkatoo-jekyll") version "$dokkatooVersion"
   }
   ```
   Or all formats

   ```kotlin
   // build.gradle.kts
  
   plugins {
     // generate all formats - HTML, Jekyll, Javadoc, and GFM (GitHub Flavoured Markdown)
     id("dev.adamko.dokkatoo") version "$dokkatooVersion"
   }
   ```
   [Read more about the available formats in the Dokka docs](https://github.com/Kotlin/dokka#output-formats).

2. Run the generation task

   ```shell
   ./gradlew :dokkatooGenerate
   ```

3. View the results in `./build/dokka/`

### Multiple Subprojects

Dokkatoo can aggregate documentation from multiple subprojects.

To do this, apply the Dokkatoo plugin in all subprojects that should be documented.

In the aggregating project, depend on the other subprojects.

```kotlin
// build.gradle.kts

plugins {
  id("dev.adamko.dokkatoo-html") version "$dokkatooVersion"
}

dependencies {
  // aggregate both subproject-hello and subproject-world
  // the subprojects must also have Dokkatoo applied
  dokkatoo(project(":subproject-hello"))
  dokkatoo(project(":subproject-world"))

  // If using Dokkatoo v2.1.0+ a dependency on all-modules-page-plugin is no longer required,
  // see https://github.com/adamko-dev/dokkatoo/issues/14
  //dokkatooPluginHtml("org.jetbrains.dokka:all-modules-page-plugin") 

  // Earlier versions of Dokkatoo must manually add a dependency:
  dokkatooPluginHtml(
    dokkatoo.versions.jetbrainsDokka.map { dokkaVersion ->
      "org.jetbrains.dokka:all-modules-page-plugin:$dokkaVersion"
    }
  )
}
```

Run the Dokkatoo generation task.

```shell
./gradlew :dokkatooGeneratePublicationHtml
```

Dokkatoo will then generate documentation into `./build/dokka/`

To improve performance only run the task in the aggregating project by prefixing the task name with
the subproject path (or `:` if aggregating in the root project).
