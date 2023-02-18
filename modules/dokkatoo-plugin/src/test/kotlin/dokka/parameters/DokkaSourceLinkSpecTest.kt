package dev.adamko.dokkatoo.dokka.parameters

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.net.URL
import org.gradle.api.Project
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.newInstance
import org.gradle.testfixtures.ProjectBuilder

class DokkaSourceLinkSpecTest : FunSpec({
  val project = ProjectBuilder.builder().build()

  context("expect localDirectoryPath") {
    test("is the invariantSeparatorsPath of localDirectory") {
      val tempDir = tempdir()

      val actual = project.createDokkaSourceLinkSpec {
        localDirectory.set(tempDir)
      }

      actual.localDirectoryPath2.get() shouldBe tempDir.invariantSeparatorsPath
    }
  }


  context("expect remoteUrl can be set") {
    test("using a string") {
      val actual = project.createDokkaSourceLinkSpec {
        remoteUrl("https://github.com/adamko-dev/dokkatoo/")
      }

      actual.remoteUrl.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }

    test("using a string-provider") {
      val actual = project.createDokkaSourceLinkSpec {
        remoteUrl(project.provider { "https://github.com/adamko-dev/dokkatoo/" })
      }

      actual.remoteUrl.get().toString() shouldBe "https://github.com/adamko-dev/dokkatoo/"
    }
  }


  context("when DokkaSourceLinkSpec is built") {

    test("expect built object contains all properties") {
      val tempDir = tempdir()

      val actual = project.createDokkaSourceLinkSpec {
        localDirectory.set(tempDir)
        remoteUrl("https://github.com/adamko-dev/dokkatoo/")
        remoteLineSuffix.set("%L")
      }

      val actualBuilt = actual.build()

      actualBuilt.should {
        it.remoteUrl shouldBe actual.remoteUrl.get()
        it.localDirectory shouldBe tempDir.invariantSeparatorsPath
        it.remoteLineSuffix shouldBe "%L"
      }
    }

    test("expect localDirectory is required") {
      val actual = project.createDokkaSourceLinkSpec {
        localDirectory.set(null as File?)
        remoteUrl("https://github.com/adamko-dev/dokkatoo/")
        remoteLineSuffix.set("%L")
      }

      val caughtException = shouldThrow<MissingValueException> {
        actual.build()
      }

      caughtException.message shouldContain "Cannot query the value of property 'localDirectory' because it has no value available"
    }

    test("expect localDirectory is an invariantSeparatorsPath") {
      val tempDir = tempdir()

      val actual = project.createDokkaSourceLinkSpec {
        localDirectory.set(tempDir)
        remoteUrl("https://github.com/adamko-dev/dokkatoo/")
        remoteLineSuffix.set(null as String?)
      }

      actual.build().should {
        it.localDirectory shouldBe tempDir.invariantSeparatorsPath
      }
    }

    test("expect remoteUrl is required") {

      val actual = project.createDokkaSourceLinkSpec {
        localDirectory.set(tempdir())
        remoteUrl.set(null as URL?)
        remoteLineSuffix.set("%L")
      }

      val caughtException = shouldThrow<MissingValueException> {
        actual.build()
      }

      caughtException.message shouldContain "Cannot query the value of property 'remoteUrl' because it has no value available"
    }

    test("expect remoteLineSuffix is optional") {
      val tempDir = tempdir()

      val actual = project.createDokkaSourceLinkSpec {
        localDirectory.set(tempDir)
        remoteUrl("https://github.com/adamko-dev/dokkatoo/")
        remoteLineSuffix.set(null as String?)
      }

      val actualBuilt = actual.build()

      actualBuilt.should {
        it.remoteUrl shouldBe actual.remoteUrl.get()
        it.localDirectory.shouldBe(tempDir.invariantSeparatorsPath)
        it.remoteLineSuffix.shouldBe(null)
      }
    }
  }
}) {
  abstract class DokkaSourceLinkSpec2 : DokkaSourceLinkSpec() {
    val localDirectoryPath2: Provider<String>
      get() = super.localDirectoryPath
  }

  companion object {
    private fun Project.createDokkaSourceLinkSpec(
      configure: DokkaSourceLinkSpec.() -> Unit
    ): DokkaSourceLinkSpec2 =
      objects.newInstance(DokkaSourceLinkSpec2::class).apply(configure)
  }
}
