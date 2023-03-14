package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.internal.DokkatooConstants.DOKKATOO_VERSION
import dev.adamko.dokkatoo.utils.*
import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.string.shouldContain

class DokkatooPluginFunctionalTest : FunSpec({
  val testProject = gradleKtsProjectTest("DokkatooPluginFunctionalTest") {
    buildGradleKts = """
      |plugins {
      |    id("dev.adamko.dokkatoo") version "$DOKKATOO_VERSION"
      |}
      |
    """.trimMargin()
  }

  test("expect Dokka Plugin creates Dokka tasks") {
    val build = testProject.runner
      .withArguments("tasks", "--group=dokkatoo", "-q")
      .build()

    withClue(build.output) {
      val dokkatooTasks = build.output
        .substringAfter("Dokkatoo tasks")
        .lines()
        .filter { it.contains(" - ") }
        .associate { it.splitToPair(" - ") }

      dokkatooTasks.shouldContainExactly(
        //@formatter:off
        "dokkatooGenerate"                       to "Generates Dokkatoo publications for all formats",
        "dokkatooGenerateModuleGfm"              to "Executes the Dokka Generator, generating a gfm module",
        "dokkatooGenerateModuleHtml"             to "Executes the Dokka Generator, generating a html module",
        "dokkatooGenerateModuleJavadoc"          to "Executes the Dokka Generator, generating a javadoc module",
        "dokkatooGenerateModuleJekyll"           to "Executes the Dokka Generator, generating a jekyll module",
        "dokkatooGeneratePublicationGfm"         to "Executes the Dokka Generator, generating the gfm publication",
        "dokkatooGeneratePublicationHtml"        to "Executes the Dokka Generator, generating the html publication",
        "dokkatooGeneratePublicationJavadoc"     to "Executes the Dokka Generator, generating the javadoc publication",
        "dokkatooGeneratePublicationJekyll"      to "Executes the Dokka Generator, generating the jekyll publication",
        "prepareDokkatooModuleDescriptorGfm"     to "Prepares the Dokka Module Descriptor for gfm",
        "prepareDokkatooModuleDescriptorHtml"    to "Prepares the Dokka Module Descriptor for html",
        "prepareDokkatooModuleDescriptorJavadoc" to "Prepares the Dokka Module Descriptor for javadoc",
        "prepareDokkatooModuleDescriptorJekyll"  to "Prepares the Dokka Module Descriptor for jekyll",
        "prepareDokkatooParameters"              to "Prepares Dokka parameters for all formats",
        "prepareDokkatooParametersGfm"           to "Prepares Dokka parameters for generating the gfm publication",
        "prepareDokkatooParametersHtml"          to "Prepares Dokka parameters for generating the html publication",
        "prepareDokkatooParametersJavadoc"       to "Prepares Dokka parameters for generating the javadoc publication",
        "prepareDokkatooParametersJekyll"        to "Prepares Dokka parameters for generating the jekyll publication",
        //@formatter:on
      )
    }
  }

  test("expect Dokka Plugin creates Dokka outgoing variants") {
    val build = testProject.runner
      .withArguments("outgoingVariants", "-q")
      .build()

    val variants = build.output.invariantNewlines().replace('\\', '/')

    val dokkatooVariants = variants.lines()
      .filter { it.contains("dokka", ignoreCase = true) }
      .mapNotNull { it.substringAfter("Variant ", "").takeIf(String::isNotBlank) }

    dokkatooVariants.shouldContainExactlyInAnyOrder(
      "dokkatooParametersElementsGfm",
      "dokkatooParametersElementsHtml",
      "dokkatooParametersElementsJavadoc",
      "dokkatooParametersElementsJekyll",
      "dokkatooModuleElementsGfm",
      "dokkatooModuleElementsHtml",
      "dokkatooModuleElementsJavadoc",
      "dokkatooModuleElementsJekyll",
    )

    fun checkVariant(format: String) {
      val formatCapitalized = format.capitalize()

      variants shouldContain /* language=text */ """
        |--------------------------------------------------
        |Variant dokkatooParametersElements$formatCapitalized
        |--------------------------------------------------
        |Provide Dokka Parameters for $format to other subprojects
        |
        |Capabilities
        |    - :test:unspecified (default capability)
        |Attributes
        |    - dev.adamko.dokkatoo.base     = dokkatoo
        |    - dev.adamko.dokkatoo.category = generator-parameters
        |    - dev.adamko.dokkatoo.format   = $format
        |Artifacts
        |    - build/dokka-config/$format/dokka_parameters.json (artifactType = json)
        |
      """.trimMargin()

      variants shouldContain /* language=text */ """
        |--------------------------------------------------
        |Variant dokkatooModuleElements$formatCapitalized
        |--------------------------------------------------
        |Provide Dokka Module files for $format to other subprojects
        |
        |Capabilities
        |    - :test:unspecified (default capability)
        |Attributes
        |    - dev.adamko.dokkatoo.base     = dokkatoo
        |    - dev.adamko.dokkatoo.category = module-files
        |    - dev.adamko.dokkatoo.format   = $format
        |Artifacts
        |    - build/dokka-config/$format/module_descriptor.json (artifactType = json)
        |    - build/dokka-module/$format (artifactType = directory)
        |
      """.trimMargin()
    }

    checkVariant("gfm")
    checkVariant("html")
    checkVariant("javadoc")
    checkVariant("jekyll")
  }

  test("expect Dokka Plugin creates Dokka resolvable configurations") {

    val expectedFormats = listOf("Gfm", "Html", "Javadoc", "Jekyll")

    val build = testProject.runner
      .withArguments("resolvableConfigurations", "-q")
      .build()

    build.output.invariantNewlines().asClue { allConfigurations ->

      val dokkatooConfigurations = allConfigurations.lines()
        .filter { it.contains("dokka", ignoreCase = true) }
        .mapNotNull { it.substringAfter("Configuration ", "").takeIf(String::isNotBlank) }

      dokkatooConfigurations.shouldContainExactlyInAnyOrder(
        mutableListOf<String>().apply {
          add("dokkatoo")

          addAll(expectedFormats.map { "dokkatooParameters$it" })
          addAll(expectedFormats.map { "dokkatooModule$it" })
          addAll(expectedFormats.map { "dokkatooGeneratorClasspath$it" })
          addAll(expectedFormats.map { "dokkatooPlugin$it" })
          addAll(expectedFormats.map { "dokkatooPluginIntransitive$it" })
        }
      )

      withClue("Configuration dokka") {
        build.output.invariantNewlines() shouldContain /* language=text */ """
          |--------------------------------------------------
          |Configuration dokkatoo
          |--------------------------------------------------
          |Fetch all Dokkatoo files from all configurations in other subprojects
          |
          |Attributes
          |    - dev.adamko.dokkatoo.base = dokkatoo
          |
        """.trimMargin()
      }

      fun checkConfigurations(format: String) {
        val formatLowercase = format.toLowerCase()

        allConfigurations shouldContain /* language=text */ """
          |--------------------------------------------------
          |Configuration dokkatooParameters$format
          |--------------------------------------------------
          |Fetch Dokka Parameters for $formatLowercase from other subprojects
          |
          |Attributes
          |    - dev.adamko.dokkatoo.base     = dokkatoo
          |    - dev.adamko.dokkatoo.category = generator-parameters
          |    - dev.adamko.dokkatoo.format   = $formatLowercase
          |Extended Configurations
          |    - dokkatoo
          |
        """.trimMargin()


        allConfigurations shouldContain /* language=text */ """
          |--------------------------------------------------
          |Configuration dokkatooGeneratorClasspath$format
          |--------------------------------------------------
          |Dokka Generator runtime classpath for $formatLowercase - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run.
          |
          |Attributes
          |    - dev.adamko.dokkatoo.base       = dokkatoo
          |    - dev.adamko.dokkatoo.category   = generator-classpath
          |    - dev.adamko.dokkatoo.format     = $formatLowercase
          |    - org.gradle.category            = library
          |    - org.gradle.dependency.bundling = external
          |    - org.gradle.jvm.environment     = standard-jvm
          |    - org.gradle.libraryelements     = jar
          |    - org.gradle.usage               = java-runtime
          |Extended Configurations
          |    - dokkatooPlugin$format
          |
       """.trimMargin()

        allConfigurations shouldContain /* language=text */ """
          |--------------------------------------------------
          |Configuration dokkatooPlugin$format
          |--------------------------------------------------
          |Dokka Plugins classpath for $formatLowercase
          |
          |Attributes
          |    - dev.adamko.dokkatoo.base       = dokkatoo
          |    - dev.adamko.dokkatoo.category   = plugins-classpath
          |    - dev.adamko.dokkatoo.format     = $formatLowercase
          |    - org.gradle.category            = library
          |    - org.gradle.dependency.bundling = external
          |    - org.gradle.jvm.environment     = standard-jvm
          |    - org.gradle.libraryelements     = jar
          |    - org.gradle.usage               = java-runtime
          |
        """.trimMargin()

        allConfigurations shouldContain /* language=text */ """
          |--------------------------------------------------
          |Configuration dokkatooPluginIntransitive$format
          |--------------------------------------------------
          |Dokka Plugins classpath for $formatLowercase - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration.
          |
          |Attributes
          |    - dev.adamko.dokkatoo.base       = dokkatoo
          |    - dev.adamko.dokkatoo.category   = plugins-classpath
          |    - dev.adamko.dokkatoo.format     = $formatLowercase
          |    - org.gradle.category            = library
          |    - org.gradle.dependency.bundling = external
          |    - org.gradle.jvm.environment     = standard-jvm
          |    - org.gradle.libraryelements     = jar
          |    - org.gradle.usage               = java-runtime
          |Extended Configurations
          |    - dokkatooPlugin$format
          |
        """.trimMargin()
      }

      expectedFormats.forEach {
        checkConfigurations(it)
      }
    }
  }
})
