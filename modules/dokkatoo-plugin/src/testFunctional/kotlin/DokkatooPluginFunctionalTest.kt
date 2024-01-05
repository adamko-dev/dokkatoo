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
      |  id("dev.adamko.dokkatoo") version "$DOKKATOO_VERSION"
      |}
      |
    """.trimMargin()
  }

  test("expect Dokka Plugin creates Dokka tasks") {
    testProject.runner
      .addArguments("tasks", "--group=dokkatoo", "-q")
      .build {
        withClue(output) {
          val dokkatooTasks = output
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
            "prepareDokkatooModuleDescriptorGfm"     to "[Deprecated ⚠️] Prepares the Dokka Module Descriptor for gfm",
            "prepareDokkatooModuleDescriptorHtml"    to "[Deprecated ⚠️] Prepares the Dokka Module Descriptor for html",
            "prepareDokkatooModuleDescriptorJavadoc" to "[Deprecated ⚠️] Prepares the Dokka Module Descriptor for javadoc",
            "prepareDokkatooModuleDescriptorJekyll"  to "[Deprecated ⚠️] Prepares the Dokka Module Descriptor for jekyll",
            //@formatter:on
          )
        }
      }
  }

  test("expect Dokka Plugin creates Dokka outgoing variants") {
    testProject.runner
      .addArguments("outgoingVariants", "-q")
      .build {
        val variants = output.invariantNewlines().replace('\\', '/')

        val dokkatooVariants = variants.lines()
          .filter { it.startsWith("Variant ") && it.contains("dokka", ignoreCase = true) }
          .mapNotNull { it.substringAfter("Variant ", "").ifBlank { null } }

        dokkatooVariants.shouldContainExactlyInAnyOrder(
          expectedFormats.flatMap {
            listOf(
              "dokkatoo${it}ModuleDirectoryConsumable",
              "dokkatoo${it}ModuleIncludesConsumable",
            )
          }
        )

        fun checkVariant(format: String) {
          @Suppress("LocalVariableName")
          val Format = format.uppercaseFirstChar()

          variants shouldContain /* language=text */ """
            |--------------------------------------------------
            |Variant dokkatoo${Format}ModuleIncludesConsumable
            |--------------------------------------------------
            |Provides Dokkatoo $format ModuleIncludes files for consumption by other subprojects.
            |
            |Capabilities
            |    - :test:unspecified (default capability)
            |Attributes
            |    - dev.adamko.dokkatoo.format           = $format
            |    - dev.adamko.dokkatoo.module-component = ModuleIncludes
            |    - dev.adamko.dokkatoo.module-name      = test
            |    - dev.adamko.dokkatoo.module-path      = 
            |    - org.gradle.usage                     = dev.adamko.dokkatoo
          """.trimMargin()

          variants shouldContain /* language=text */ """
            |--------------------------------------------------
            |Variant dokkatoo${Format}ModuleDirectoryConsumable
            |--------------------------------------------------
            |Provides Dokkatoo $format ModuleDirectory files for consumption by other subprojects.
            |
            |Capabilities
            |    - :test:unspecified (default capability)
            |Attributes
            |    - dev.adamko.dokkatoo.format                    = $format
            |    - dev.adamko.dokkatoo.module-component          = ModuleDirectory
            |    - dev.adamko.dokkatoo.module-generate-task-path = :dokkatooGenerateModule$Format
            |    - dev.adamko.dokkatoo.module-name               = test
            |    - dev.adamko.dokkatoo.module-path               = 
            |    - org.gradle.usage                              = dev.adamko.dokkatoo
            |Artifacts
            |    - build/dokka-module/$format (artifactType = directory)
          """.trimMargin()
        }

        checkVariant("gfm")
        checkVariant("html")
        checkVariant("javadoc")
        checkVariant("jekyll")
      }
  }

  test("expect Dokka Plugin creates Dokka resolvable configurations") {

    testProject.runner
      .addArguments("resolvableConfigurations", "-q")
      .build {
        output.invariantNewlines().asClue { allConfigurations ->

          val dokkatooConfigurations = allConfigurations.lines()
            .filter { it.contains("dokka", ignoreCase = true) }
            .mapNotNull { it.substringAfter("Configuration ", "").takeIf(String::isNotBlank) }

          dokkatooConfigurations.shouldContainExactlyInAnyOrder(
            buildSet {
              addAll(expectedFormats.map { "dokkatoo${it}Resolver" })
              addAll(expectedFormats.map { "dokkatoo${it}GeneratorClasspathResolver" })
              addAll(expectedFormats.map { "dokkatoo${it}PluginsClasspathIntransitiveResolver" })
              addAll(expectedFormats.map { "dokkatoo${it}ModuleDirectoryResolver" })
              addAll(expectedFormats.map { "dokkatoo${it}ModuleIncludesResolver" })
            }
          )

          fun checkConfigurations(
            @Suppress("LocalVariableName")
            Format: String
          ) {
            val format = Format.lowercase()

            allConfigurations shouldContain /* language=text */ """
              |--------------------------------------------------
              |Configuration dokkatoo${Format}Resolver
              |--------------------------------------------------
              |Resolve Dokkatoo declared dependencies for $format.
              |
              |Attributes
              |    - dev.adamko.dokkatoo.format      = $format
              |    - dev.adamko.dokkatoo.module-name = test
              |    - dev.adamko.dokkatoo.module-path = 
              |    - org.gradle.usage                = dev.adamko.dokkatoo
              |Extended Configurations
              |    - dokkatoo${Format}
           """.trimMargin()

            allConfigurations shouldContain /* language=text */ """
              |--------------------------------------------------
              |Configuration dokkatoo${Format}GeneratorClasspathResolver
              |--------------------------------------------------
              |Dokka Generator runtime classpath for $format - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run.
              |
              |Attributes
              |    - dev.adamko.dokkatoo.classpath  = dokka-generator
              |    - dev.adamko.dokkatoo.format     = $format
              |    - org.gradle.category            = library
              |    - org.gradle.dependency.bundling = external
              |    - org.gradle.jvm.environment     = standard-jvm
              |    - org.gradle.libraryelements     = jar
              |    - org.gradle.usage               = java-runtime
              |Extended Configurations
              |    - dokkatoo${Format}GeneratorClasspath
           """.trimMargin()

            allConfigurations shouldContain /* language=text */ """
              |--------------------------------------------------
              |Configuration dokkatoo${Format}PluginsClasspathIntransitiveResolver
              |--------------------------------------------------
              |Resolves Dokka Plugins classpath for $format - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration.
              |
              |Attributes
              |    - dev.adamko.dokkatoo.classpath  = dokka-plugins
              |    - dev.adamko.dokkatoo.format     = $format
              |    - org.gradle.category            = library
              |    - org.gradle.dependency.bundling = external
              |    - org.gradle.jvm.environment     = standard-jvm
              |    - org.gradle.libraryelements     = jar
              |    - org.gradle.usage               = java-runtime
              |Extended Configurations
              |    - dokkatooPlugin${Format}
           """.trimMargin()

            allConfigurations shouldContain /* language=text */ """
              |--------------------------------------------------
              |Configuration dokkatoo${Format}ModuleDirectoryResolver
              |--------------------------------------------------
              |Resolves Dokkatoo $format ModuleDirectory files.
              |
              |Attributes
              |    - dev.adamko.dokkatoo.format           = $format
              |    - dev.adamko.dokkatoo.module-component = ModuleDirectory
              |    - dev.adamko.dokkatoo.module-name      = test
              |    - dev.adamko.dokkatoo.module-path      = 
              |    - org.gradle.usage                     = dev.adamko.dokkatoo
              |Extended Configurations
              |    - dokkatoo${Format}Resolver
            """.trimMargin()

            allConfigurations shouldContain /* language=text */ """
              |--------------------------------------------------
              |Configuration dokkatoo${Format}ModuleIncludesResolver
              |--------------------------------------------------
              |Resolves Dokkatoo $format ModuleIncludes files.
              |
              |Attributes
              |    - dev.adamko.dokkatoo.format           = $format
              |    - dev.adamko.dokkatoo.module-component = ModuleIncludes
              |    - dev.adamko.dokkatoo.module-name      = test
              |    - dev.adamko.dokkatoo.module-path      = 
              |    - org.gradle.usage                     = dev.adamko.dokkatoo
              |Extended Configurations
              |    - dokkatoo${Format}Resolver
            """.trimMargin()
          }

          expectedFormats.forEach {
            checkConfigurations(it)
          }
        }
      }
  }
}) {
  companion object {
    private val expectedFormats = listOf("Gfm", "Html", "Javadoc", "Jekyll")
  }
}
