package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.utils.create_
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.testfixtures.ProjectBuilder


class DokkaExternalDocumentationLinkSpecTest : FunSpec({
  val project = ProjectBuilder.builder().build()

  context("expect url can be set") {
    test("using a string") {
      val actual = project.createExternalDocLinkSpec("test") {
        url("https://github.com/adamko-dev/dokkatoo/")
      }

      actual.url.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }

    test("using a string-provider") {
      val actual = project.createExternalDocLinkSpec("test") {
        url(project.provider { "https://github.com/adamko-dev/dokkatoo/" })
      }

      actual.url.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }
  }

  context("expect packageListUrl can be set") {
    test("using a string") {
      val actual = project.createExternalDocLinkSpec("test") {
        packageListUrl("https://github.com/adamko-dev/dokkatoo/")
      }

      actual.packageListUrl.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }

    test("using a string-provider") {
      val actual = project.createExternalDocLinkSpec("test") {
        packageListUrl(project.provider { "https://github.com/adamko-dev/dokkatoo/" })
      }

      actual.packageListUrl.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }
  }
})

private fun Project.createExternalDocLinkSpec(
  name: String,
  configure: DokkaExternalDocumentationLinkSpec.() -> Unit
): DokkaExternalDocumentationLinkSpec =
  objects
    .domainObjectContainer(DokkaExternalDocumentationLinkSpec::class)
    .create_(name, configure)
