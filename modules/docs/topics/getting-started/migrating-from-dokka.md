# Migrating from Dokka

For help in migrating from the Dokka Gradle Plugin to Dokkatoo, you can still apply both plugins -
just make sure to update the Dokkatoo output directory, so it doesn't overlap!

```kotlin
// build.gradle.kts

plugins {
  id("org.jetbrains.dokka") version "$dokkaVersion"
  id("dev.adamko.dokkatoo-html") version "$dokkatooVersion"
}

dokkatoo {
  // update the output directory, so it doesn't clash with the Dokka plugin! 
  dokkatooPublicationDirectory.set(layout.buildDirectory.dir("dokkatoo"))
}
```
