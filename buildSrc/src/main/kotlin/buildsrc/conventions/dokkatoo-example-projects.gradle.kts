package buildsrc.conventions

import buildsrc.settings.DokkatooExampleProjectsSettings
import buildsrc.settings.MavenPublishTestSettings
import buildsrc.tasks.*
import buildsrc.utils.*
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
  id("buildsrc.conventions.base")
  id("buildsrc.conventions.dokka-source-downloader")
  id("buildsrc.conventions.maven-publish-test")
  id("buildsrc.conventions.dokkatoo-example-projects-base")
}

val prepareDokkaSourceTask = tasks.named<Sync>("prepareDokkaSource")

val setupDokkaTemplateProjects by tasks.registering(SetupDokkaProjects::class) {
  group = DokkatooExampleProjectsSettings.TASK_GROUP

  dependsOn(prepareDokkaSourceTask)

  // complicated workaround for https://github.com/gradle/gradle/issues/23708
  val layout = serviceOf<ProjectLayout>()
  val providers = serviceOf<ProviderFactory>()

  val dokkaSrcDir = prepareDokkaSourceTask.flatMap {
    layout.dir(providers.provider {
      it.destinationDir
    })
  }
  dokkaSourceDir.set(dokkaSrcDir)

  destinationToSources.convention(emptyMap())
}

val mavenPublishTestExtension = extensions.getByType<MavenPublishTestSettings>()


fun createDokkatooExampleProjectsSettings(
  projectDir: Directory = project.layout.projectDirectory
): DokkatooExampleProjectsSettings {
  return extensions.create<DokkatooExampleProjectsSettings>(
    DokkatooExampleProjectsSettings.EXTENSION_NAME
  ).apply {

    // find all Gradle settings files
    val settingsFiles = projectDir.asFileTree
      .matching {
        include(
          "**/*dokkatoo*/settings.gradle.kts",
          "**/*dokkatoo*/settings.gradle",
        )
      }.files

    // for each settings file, create a GradlePropertiesSpec
    settingsFiles.forEach {
      val destinationDir = it.parentFile
      val name = destinationDir.toRelativeString(projectDir.asFile).toAlphaNumericCamelCase()
      gradleProperties.register(name) {
        this.destinationDir.set(destinationDir)
      }
    }

    gradleProperties.configureEach {
      content.add(
        mavenPublishTestExtension.testMavenRepoPath.map { testMavenRepoPath ->
          "testMavenRepo=$testMavenRepoPath"
        }
      )
    }
  }
}

val dokkatooExampleProjectsSettings = createDokkatooExampleProjectsSettings()

val updateDokkatooExamplesGradleProperties by tasks.registering(
  UpdateDokkatooExampleGradleProperties::class
) {
  group = DokkatooExampleProjectsSettings.TASK_GROUP

  mustRunAfter(tasks.withType<SetupDokkaProjects>())

  gradleProperties.addAllLater(providers.provider { dokkatooExampleProjectsSettings.gradleProperties })
}

val dokkatooVersion = provider { project.version.toString() }

val updateDokkatooExamplesBuildFiles by tasks.registering {
  group = DokkatooExampleProjectsSettings.TASK_GROUP
  description = "Update the Gradle build files in the Dokkatoo examples"

  outputs.upToDateWhen { false }

  mustRunAfter(tasks.withType<SetupDokkaProjects>())
  shouldRunAfter(updateDokkatooExamplesGradleProperties)

  val dokkatooVersion = dokkatooVersion

  val dokkatooPluginVersionMatcher = """
    id[^"]+?\"dev\.adamko\.dokkatoo\".+?version \"([^"]+?)\"
    """.trimIndent().toRegex()

  val gradleBuildFiles =
    layout.projectDirectory.asFileTree
      .matching {
        include(
          "**/*dokkatoo*/**/build.gradle.kts",
          "**/*dokkatoo*/**/build.gradle",
        )
      }.elements

  outputs.files(gradleBuildFiles)

  doLast {
    gradleBuildFiles.get().forEach {
      val file = it.asFile
      if (file.exists()) {
        file.writeText(
          file.readText().replace(dokkatooPluginVersionMatcher) {
            val oldVersion = it.groupValues[1]
            it.value.replace(oldVersion, dokkatooVersion.get())
          })
      }
    }
  }
}


val updateDokkatooExamples by tasks.registering {
  group = DokkatooExampleProjectsSettings.TASK_GROUP
  description = "lifecycle task for all '${DokkatooExampleProjectsSettings.TASK_GROUP}' tasks"
  dependsOn(
    setupDokkaTemplateProjects,
    updateDokkatooExamplesGradleProperties,
    updateDokkatooExamplesBuildFiles,
  )
}

tasks.assemble {
  dependsOn(updateDokkatooExamples)
}
