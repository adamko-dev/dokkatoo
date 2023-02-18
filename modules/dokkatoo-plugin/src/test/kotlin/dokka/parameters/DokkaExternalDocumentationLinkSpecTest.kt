package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.create_
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.testfixtures.ProjectBuilder


class DokkaExternalDocumentationLinkSpecTest : FunSpec({

  context("expect url can be set") {
    test("using a string") {
      val project = ProjectBuilder.builder().build()

      val container = project.objects
        .domainObjectContainer(DokkaExternalDocumentationLinkSpec::class)

      val actual = container.create_("test") {
        url("https://github.com/adamko-dev/dokkatoo/")
      }

      actual.url.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }

    test("using a string-provider") {
      val project = ProjectBuilder.builder().build()

      val container = project.objects
        .domainObjectContainer(DokkaExternalDocumentationLinkSpec::class)

      val actual = container.create_("test") {
        url(project.provider { "https://github.com/adamko-dev/dokkatoo/" })
      }

      actual.url.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }
  }

  context("expect packageListUrl can be set") {
    test("using a string") {
      val project = ProjectBuilder.builder().build()

      val container = project.objects
        .domainObjectContainer(DokkaExternalDocumentationLinkSpec::class)

      val actual = container.create_("test") {
        packageListUrl("https://github.com/adamko-dev/dokkatoo/")
      }

      actual.packageListUrl.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }

    test("using a string-provider") {
      val project = ProjectBuilder.builder().build()

      val container = project.objects
        .domainObjectContainer(DokkaExternalDocumentationLinkSpec::class)

      val actual = container.create_("test") {
        packageListUrl(project.provider { "https://github.com/adamko-dev/dokkatoo/" })
      }

      actual.packageListUrl.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }
  }
})
