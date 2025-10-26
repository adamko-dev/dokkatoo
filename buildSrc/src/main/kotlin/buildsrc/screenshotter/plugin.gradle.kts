package buildsrc.screenshotter

import buildsrc.utils.declarable
import buildsrc.utils.resolvable
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE


val screenshotterClasspath: Configuration by configurations.creating {
  declarable()
  withDependencies {
    add(project.dependencies.create("com.microsoft.playwright:playwright:1.55.0"))
  }
}

val screenshotterClasspathResolver: Configuration by configurations.creating {
  resolvable()
  extendsFrom(screenshotterClasspath)
  attributes {
    attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
  }
}

val screenshotter by tasks.registering(ScreenshotTask::class) {
  runtimeClasspath.from(screenshotterClasspathResolver.incoming.files)

  websites.configureEach {
    enabled.convention(true)
  }

  outputDirectory.set(temporaryDir)
}
