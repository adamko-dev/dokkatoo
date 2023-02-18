package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.create_
import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.net.URL
import org.gradle.api.Project
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.domainObjectContainer
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

  context("when building a ExternalDocumentationLinkKxs") {
    test("expect url is required") {
      val actual = project.createExternalDocLinkSpec("test") {
        url.set(null as URL?)
        packageListUrl("https://github.com/adamko-dev/dokkatoo/")
      }

      val caughtException = shouldThrow<MissingValueException> {
        actual.build()
      }

      caughtException.message shouldContain "Cannot query the value of property 'url' because it has no value available"
    }
    test("expect packageListUrl is required") {
      val actual = project.createExternalDocLinkSpec("test") {
        url("https://github.com/adamko-dev/dokkatoo/")
        packageListUrl.set(null as URL?)
      }

      val caughtException = shouldThrow<MissingValueException> {
        actual.build()
      }

      caughtException.message shouldContain "Cannot query the value of property 'packageListUrl' because it has no value available"
    }

    test("expect null when not enabled") {

      val actual = project.createExternalDocLinkSpec("test") {

        fun <T> failingProvider(propertyName: String): Provider<T> = project.provider {
          fail("ExternalDocLink is disabled - $propertyName should not be queried")
        }

        url(failingProvider("url"))
        packageListUrl(failingProvider("packageListUrl"))

        enabled.set(false)
      }

      actual.build() shouldBe null
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
