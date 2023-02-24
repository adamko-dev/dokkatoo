package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.utils.buildGradleKts
import dev.adamko.dokkatoo.utils.gradleKtsProjectTest
import dev.adamko.dokkatoo.utils.invariantNewlines
import io.kotest.assertions.withClue
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.Test

class DokkatooPluginFunctionalTest {
  private val testProject = gradleKtsProjectTest("DokkatooPluginFunctionalTest") {
    buildGradleKts = """
        |plugins {
        |    id("dev.adamko.dokkatoo") version "0.0.2-SNAPSHOT"
        |}
      """.trimMargin()
  }

  @Test
  fun `expect Dokka Plugin creates Dokka tasks`() {
    val build = testProject.runner
      .withArguments("tasks")
      .build()

    build.output.invariantNewlines() shouldContain /* language=text */ """
      |Dokkatoo tasks
      |--------------
      |dokkatooGenerate - Runs all Dokkatoo Generate tasks
      |dokkatooGenerateModuleGfm - Executes the Dokka Generator, generating a gfm module
      |dokkatooGenerateModuleHtml - Executes the Dokka Generator, generating a html module
      |dokkatooGenerateModuleJavadoc - Executes the Dokka Generator, generating a javadoc module
      |dokkatooGenerateModuleJekyll - Executes the Dokka Generator, generating a jekyll module
      |dokkatooGeneratePublicationGfm - Executes the Dokka Generator, generating the gfm publication
      |dokkatooGeneratePublicationHtml - Executes the Dokka Generator, generating the html publication
      |dokkatooGeneratePublicationJavadoc - Executes the Dokka Generator, generating the javadoc publication
      |dokkatooGeneratePublicationJekyll - Executes the Dokka Generator, generating the jekyll publication
      |prepareDokkatooModuleDescriptorGfm - Prepares the Dokka Module Descriptor for gfm
      |prepareDokkatooModuleDescriptorHtml - Prepares the Dokka Module Descriptor for html
      |prepareDokkatooModuleDescriptorJavadoc - Prepares the Dokka Module Descriptor for javadoc
      |prepareDokkatooModuleDescriptorJekyll - Prepares the Dokka Module Descriptor for jekyll
      |prepareDokkatooParameters - Runs all Dokkatoo Create Configuration tasks
      |prepareDokkatooParametersGfm - Creates Dokka Configuration for executing the Dokka Generator for the gfm publication
      |prepareDokkatooParametersHtml - Creates Dokka Configuration for executing the Dokka Generator for the html publication
      |prepareDokkatooParametersJavadoc - Creates Dokka Configuration for executing the Dokka Generator for the javadoc publication
      |prepareDokkatooParametersJekyll - Creates Dokka Configuration for executing the Dokka Generator for the jekyll publication
      |
    """.trimMargin()
  }

  @Test
  fun `expect Dokka Plugin creates Dokka outgoing variants`() {
    val build = testProject.runner
      .withArguments("outgoingVariants")
      .build()


    fun checkVariant(format: String) {
      val formatCapitalized = format.capitalize()

      build.output.invariantNewlines() shouldContain /* language=text */ """
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
      """.trimMargin().replace('/', File.separatorChar)

      build.output.invariantNewlines() shouldContain /* language=text */ """
        |--------------------------------------------------
        |Variant dokkatooPluginElements$formatCapitalized
        |--------------------------------------------------
        |Provide the Dokka Plugins classpath for $format to other subprojects
        |
        |Capabilities
        |    - :test:unspecified (default capability)
        |Attributes
        |    - dev.adamko.dokkatoo.base       = dokkatoo
        |    - dev.adamko.dokkatoo.category   = plugins-classpath
        |    - dev.adamko.dokkatoo.format     = $format
        |    - org.gradle.category            = library
        |    - org.gradle.dependency.bundling = external
        |    - org.gradle.jvm.environment     = standard-jvm
        |    - org.gradle.libraryelements     = jar
        |    - org.gradle.usage               = java-runtime
        |
      """.trimMargin().replace('/', File.separatorChar)

      build.output.invariantNewlines() shouldContain /* language=text */ """
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
      """.trimMargin().replace('/', File.separatorChar)
    }

    checkVariant("gfm")
    checkVariant("html")
    checkVariant("javadoc")
    checkVariant("jekyll")
  }

  @Test
  fun `expect Dokka Plugin creates Dokka resolvable configurations`() {
    val build = testProject.runner
      .withArguments("resolvableConfigurations")
      .build()

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
      val formatCapitalized = format.capitalize()

      build.output.invariantNewlines() shouldContain /* language=text */ """
        |--------------------------------------------------
        |Configuration dokkatooParameters$formatCapitalized
        |--------------------------------------------------
        |Fetch Dokka Parameters for $format from other subprojects
        |
        |Attributes
        |    - dev.adamko.dokkatoo.base     = dokkatoo
        |    - dev.adamko.dokkatoo.category = generator-parameters
        |    - dev.adamko.dokkatoo.format   = $format
        |Extended Configurations
        |    - dokkatoo
        |
      """.trimMargin()


      build.output.invariantNewlines() shouldContain /* language=text */ """
        |--------------------------------------------------
        |Configuration dokkatooGeneratorClasspath$formatCapitalized
        |--------------------------------------------------
        |Dokka Generator runtime classpath for $format - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run.
        |
        |Attributes
        |    - dev.adamko.dokkatoo.base       = dokkatoo
        |    - dev.adamko.dokkatoo.category   = generator-classpath
        |    - dev.adamko.dokkatoo.format     = $format
        |    - org.gradle.category            = library
        |    - org.gradle.dependency.bundling = external
        |    - org.gradle.jvm.environment     = standard-jvm
        |    - org.gradle.libraryelements     = jar
        |    - org.gradle.usage               = java-runtime
        |Extended Configurations
        |    - dokkatooPlugin$formatCapitalized
        |
     """.trimMargin()

      build.output.invariantNewlines() shouldContain /* language=text */ """
        |--------------------------------------------------
        |Configuration dokkatooPlugin$formatCapitalized
        |--------------------------------------------------
        |Dokka Plugins classpath for $format
        |
        |Attributes
        |    - dev.adamko.dokkatoo.base       = dokkatoo
        |    - dev.adamko.dokkatoo.category   = plugins-classpath
        |    - dev.adamko.dokkatoo.format     = $format
        |    - org.gradle.category            = library
        |    - org.gradle.dependency.bundling = external
        |    - org.gradle.jvm.environment     = standard-jvm
        |    - org.gradle.libraryelements     = jar
        |    - org.gradle.usage               = java-runtime
        |
      """.trimMargin()

      build.output.invariantNewlines() shouldContain /* language=text */ """
        |--------------------------------------------------
        |Configuration dokkatooPluginIntransitive$formatCapitalized
        |--------------------------------------------------
        |Dokka Plugins classpath for $format - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration.
        |
        |Attributes
        |    - dev.adamko.dokkatoo.base       = dokkatoo
        |    - dev.adamko.dokkatoo.category   = plugins-classpath
        |    - dev.adamko.dokkatoo.format     = $format
        |    - org.gradle.category            = library
        |    - org.gradle.dependency.bundling = external
        |    - org.gradle.jvm.environment     = standard-jvm
        |    - org.gradle.libraryelements     = jar
        |    - org.gradle.usage               = java-runtime
        |Extended Configurations
        |    - dokkatooPlugin$formatCapitalized
        |
      """.trimMargin()
    }

    checkConfigurations("gfm")
    checkConfigurations("html")
    checkConfigurations("javadoc")
    checkConfigurations("jekyll")
  }
}
