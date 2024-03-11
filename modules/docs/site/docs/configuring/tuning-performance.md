# Tuning Performance

Hints and tips for improving build performance.

### Enable Caching

Gradle Build Cache and Configuration Cache can massively improve the performance of a build.

See the Gradle Docs for more information.

Dokkatoo is fully compatible with the Gradle, so make sure to enable it to get the most benefit.

### Worker API

Dokkatoo uses the Gradle Worker API to run Dokka Generator.
This can operate in one of two modes: process isolation, or classpath isolation.

#### Process isolation

```kotlin title="build.gradle.kts"
dokkatoo {
  dokkaGeneratorIsolation.set(
    ProcessIsolation {
      debug.set(false)
      enableAssertions.set(true)
      minHeapSize.set("512m")
      maxHeapSize.set("1g")
      // ...
    }
  )
}
```

#### Classpath isolation

```kotlin title="build.gradle.kts"
dokkatoo {
  // run Dokka Generator in the current Gradle build process
  dokkaGeneratorIsolation.set(
    ClassLoaderIsolation()
  )
}
```
