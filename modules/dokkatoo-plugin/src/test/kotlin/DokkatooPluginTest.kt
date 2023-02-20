package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Action
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder

class DokkatooPluginTest : FunSpec({

  test("expect plugin id can be applied to project successfully") {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("dev.adamko.dokkatoo")
    project.plugins.hasPlugin("dev.adamko.dokkatoo") shouldBe true
    project.plugins.hasPlugin(DokkatooPlugin::class) shouldBe true
  }

  test("expect plugin class can be applied to project successfully") {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply(type = DokkatooPlugin::class)
    project.plugins.hasPlugin("dev.adamko.dokkatoo") shouldBe true
    project.plugins.hasPlugin(DokkatooPlugin::class) shouldBe true
  }

  test("dokka configuration task") {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("dev.adamko.dokkatoo")

    project.tasks.withType<DokkatooPrepareParametersTask>().configureEach(action {
      dokkaSourceSets.create("Blah", action {

        sourceSetScope.set("moduleName")
        classpath.setFrom(emptyList<String>())
        sourceRoots.from(project.file("src/main/kotlin"))
        samples.from(emptyList<String>())
        includes.from(emptyList<String>())

        //classpath = emptyList()
        //sourceRoots = setOf(file("src/main/kotlin"))
        //dependentSourceSets = emptySet()
        //samples = emptySet()
        //includes = emptySet()
        //documentedVisibilities = DokkaConfiguration.Visibility.values().toSet()
        //reportUndocumented = false
        //skipEmptyPackages = true
        //skipDeprecated = false
        //jdkVersion = 8
        //sourceLinks = emptySet()
        //perPackageOptions = emptyList()
        //externalDocumentationLinks = emptySet()
        //languageVersion = null
        //apiVersion = null
        //noStdlibLink = false
        //noJdkLink = false
        //suppressedFiles = emptySet()
        //analysisPlatform = org.jetbrains.dokka.Platform.DEFAULT
      })
    })
  }
})

@Suppress("ObjectLiteralToLambda") // workaround for https://youtrack.jetbrains.com/issue/KTIJ-14684
private inline fun <T : Any> action(crossinline block: T.() -> Unit) =
  object : Action<T> {
    override fun execute(t: T) = t.block()
  }
