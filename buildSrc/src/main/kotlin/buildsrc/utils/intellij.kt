package buildsrc.utils

import java.io.ByteArrayOutputStream
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RelativePath
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.specs.NotSpec
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.kotlin.dsl.register
import org.gradle.plugins.ide.idea.model.IdeaModule
import org.gradle.process.ExecSpec


/** exclude generated Gradle code, so it doesn't clog up search results */
fun IdeaModule.excludeGeneratedGradleDsl(layout: ProjectLayout) {

  val generatedSrcDirs = listOf(
    "kotlin-dsl-accessors",
    "kotlin-dsl-external-plugin-spec-builders",
    "kotlin-dsl-plugins",
  )

  excludeDirs.addAll(
    layout.projectDirectory.asFile.walk()
      .filter { it.isDirectory }
      .filter { it.parentFile.name in generatedSrcDirs }
      .flatMap { file ->
        file.walk().maxDepth(1).filter { it.isDirectory }.toList()
      }
  )
}
