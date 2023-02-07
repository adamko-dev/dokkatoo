package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.utils.buildGradleKts
import dev.adamko.dokkatoo.utils.gradleKtsProjectTest
import io.kotest.assertions.withClue
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.Test

class DokkatooPluginFunctionalTest {

  @Test
  fun `expect Dokka Plugin creates Dokka tasks`() {
    val build = gradleKtsProjectTest {
      buildGradleKts = """
                plugins {
                    id("dev.adamko.dokkatoo") version "0.0.1-SNAPSHOT"
                }
            """.trimIndent()
    }.runner
      .withArguments("tasks")
      .build()

    build.output shouldContain /* language=text */ """
            |Dokkatoo tasks
            |--------------
            |createDokkatooConfiguration - Runs all Dokkatoo Create Configuration tasks
            |createDokkatooConfigurationGfm - Creates Dokka Configuration for executing the Dokka Generator for the gfm publication
            |createDokkatooConfigurationHtml - Creates Dokka Configuration for executing the Dokka Generator for the html publication
            |createDokkatooConfigurationJavadoc - Creates Dokka Configuration for executing the Dokka Generator for the javadoc publication
            |createDokkatooConfigurationJekyll - Creates Dokka Configuration for executing the Dokka Generator for the jekyll publication
            |dokkatooGenerate - Runs all Dokkatoo Generate tasks
            |dokkatooGenerateGfm - Executes the Dokka Generator, producing the gfm publication
            |dokkatooGenerateHtml - Executes the Dokka Generator, producing the html publication
            |dokkatooGenerateJavadoc - Executes the Dokka Generator, producing the javadoc publication
            |dokkatooGenerateJekyll - Executes the Dokka Generator, producing the jekyll publication
            |
        """.trimMargin()
  }

  @Test
  fun `expect Dokka Plugin creates Dokka outgoing variants`() {
    val build = gradleKtsProjectTest {
      buildGradleKts = """
                plugins {
                    id("dev.adamko.dokkatoo") version "0.0.1-SNAPSHOT"
                }
            """.trimIndent()
    }.runner
      .withArguments("outgoingVariants")
      .build()


    fun checkVariant(format: String) {
      val formatCapitalized = format.capitalize()

      build.output shouldContain /* language=text */ """
                |--------------------------------------------------
                |Variant dokkatooConfigurationElements$formatCapitalized
                |--------------------------------------------------
                |Provide Dokka Generator Configuration files for $format to other subprojects
                |
                |Capabilities
                |    - :test:unspecified (default capability)
                |Attributes
                |    - dev.adamko.dokkatoo.base     = dokkatoo
                |    - dev.adamko.dokkatoo.category = configuration
                |    - dev.adamko.dokkatoo.format   = $format
                |Artifacts
                |    - build/dokka-config/$format/dokka_configuration.json (artifactType = json)
                |
            """.trimMargin().replace("/", File.pathSeparator)
    }

    checkVariant("gfm")
    checkVariant("html")
    checkVariant("javadoc")
    checkVariant("jekyll")
  }

  @Test
  fun `expect Dokka Plugin creates Dokka resolvable configurations`() {
    val build = gradleKtsProjectTest {
      buildGradleKts = """
                plugins {
                    id("dev.adamko.dokkatoo") version "0.0.1-SNAPSHOT"
                }
            """.trimIndent()
    }.runner
      .withArguments("resolvableConfigurations")
      .build()

    withClue("Configuration dokka") {
      build.output shouldContain /* language=text */ """
                |--------------------------------------------------
                |Configuration dokkatoo
                |--------------------------------------------------
                |Fetch all Dokkatoo files from all configurations in other subprojects
                |
                |Attributes
                |    - dev.adamko.dokkatoo.base = dokka
            """.trimMargin()
    }

    fun checkConfigurations(format: String) {
      val formatCapitalized = format.capitalize()

      build.output shouldContain /* language=text */ """
                |--------------------------------------------------
                |Configuration dokkatooConfiguration$formatCapitalized
                |--------------------------------------------------
                |Fetch Dokka Generator Configuration files for $format from other subprojects
                |
                |Attributes
                |    - dev.adamko.dokkatoo.base     = dokkatoo
                |    - dev.adamko.dokkatoo.category = configuration
                |    - dev.adamko.dokkatoo.format   = $format
                |Extended Configurations
                |    - dokkatoo
            """.trimMargin()


      build.output shouldContain /* language=text */ """
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
            """.trimMargin()

      build.output shouldContain /* language=text */ """
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
            """.trimMargin()

      build.output shouldContain /* language=text */ """
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
            """.trimMargin()
    }

    checkConfigurations("gfm")
    checkConfigurations("html")
    checkConfigurations("javadoc")
    checkConfigurations("jekyll")
  }
}
