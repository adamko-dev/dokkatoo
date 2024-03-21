# Tuning Performance

Hints and tips for improving build performance.

## Enable Caching

Gradle
[Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
and
[Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
can massively improve the performance of a build.

See the Gradle Documentation for more information.

Dokkatoo is fully compatible with these Gradle features,
so make sure to enable them to get the most benefit.

## Worker API

Dokkatoo uses the Gradle Worker API to run Dokka Generator.
This can operate in one of two modes: process isolation, or classpath isolation.

### Process isolation

```kotlin title="build.gradle.kts"
dokkatoo {
  dokkaGeneratorIsolation.set(
    ProcessIsolation {}
  )
}
```

Additionally, the Java process settings can be tweaked.
For example, larger projects typically need a larger heap size.

```kotlin title="build.gradle.kts"
dokkatoo {
  dokkaGeneratorIsolation.set(
    ProcessIsolation {
      maxHeapSize.set("2g")
      minHeapSize.set("512m")
    }
  )
}
```

### Classpath isolation

Run Dokka Generator in the current Gradle build process.

```kotlin title="build.gradle.kts"
dokkatoo {
  dokkaGeneratorIsolation.set(
    ClassLoaderIsolation()
  )
}
```
