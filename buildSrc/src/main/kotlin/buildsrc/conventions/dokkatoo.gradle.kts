package buildsrc.conventions

import org.gradle.api.tasks.PathSensitivity.NONE
import org.gradle.kotlin.dsl.*

plugins {
  id("dev.adamko.dokkatoo-html")
  id("buildsrc.conventions.dokka-source-downloader")
}

val prepareDokkaTemplates by tasks.registering {
  description = "Prepare Dokka FTL templates"
  // This task is too complicated, but Dokka doesn't provide a way of injecting a single script.
  // I don't want to copy-paste the current Dokka templates into this project, because if Dokka
  // updates the templates then I won't realise they've changed.

  val outputDir = layout.buildDirectory.dir("dokkaConfig/templates")
  outputs.dir(outputDir).withPropertyName("outputDir")

  outputs.cacheIf("always cache, to avoid downloading Dokka source") { true }

  val dokkaSourceDir = tasks.named<Sync>("prepareDokkaSource").map { it.destinationDir }

  inputs.dir(dokkaSourceDir).withPropertyName("dokkaSourceDir")
    .withPathSensitivity(NONE)

  val cloudflareScript = """
    <script src="https://static.cloudflareinsights.com/beacon.min.js" data-cf-beacon='{"token": "60b3e3c8134343cc8cb3b0fa0bae9a28"}'></script>
  """.trimIndent()
  inputs.property("cloudflareScript", cloudflareScript)

  doLast {
    val pageMetadataFtl = outputDir.get().asFile.resolve("includes/page_metadata.ftl").apply {
      parentFile.mkdirs()
    }

    val sourcePageMetadataFtl = dokkaSourceDir.get()
      .resolve("dokka-subprojects/plugin-base/src/main/resources/dokka/templates/includes/page_metadata.ftl")

    pageMetadataFtl.writeText(
      "<#-- DO NOT EDIT generated by $path -->\n" +
          sourcePageMetadataFtl.readText()
            .replace(
              """</title>""",
              """</title>${"\n"}    $cloudflareScript""",
            )
    )

    require(cloudflareScript in pageMetadataFtl.readText()) {
      "Cloudflare script missing from $pageMetadataFtl"
    }
  }
}

dokkatoo {

  dokkatooSourceSets.configureEach {
    externalDocumentationLinks.register("gradle") {
      // https://docs.gradle.org/current/javadoc/index.html
      url("https://docs.gradle.org/${gradle.gradleVersion}/javadoc/")
    }

    sourceLink {
      localDirectory.set(file("src/main/kotlin"))
      val relativeProjectPath = projectDir.relativeToOrNull(rootDir)?.invariantSeparatorsPath ?: ""
      remoteUrl("https://github.com/adamko-dev/dokkatoo/tree/main/$relativeProjectPath/src/main/kotlin")
    }
  }

  pluginsConfiguration {
    html {
      templatesDir.fileProvider(prepareDokkaTemplates.map { it.outputs.files.singleFile })
      homepageLink = "https://github.com/adamko-dev/dokkatoo/"
      footerMessage = "Copyright © 2023"
    }
  }
}
