import dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier
import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters

plugins {
  kotlin("jvm") version "1.9.0"
  id("dev.adamko.dokkatoo") version "2.1.0-SNAPSHOT"
}

version = "1.9.0-SNAPSHOT"

dependencies {
  testImplementation(kotlin("test-junit"))
}

kotlin {
  jvmToolchain(8)
}

dokkatoo {
  moduleName = "Basic Project"
  dokkatooSourceSets.configureEach {
    documentedVisibilities(
      VisibilityModifier.PUBLIC,
      VisibilityModifier.PROTECTED,
    )
    suppressedFiles.from(file("src/main/kotlin/it/suppressedByPath"))
    perPackageOption {
      matchingRegex = "it.suppressedByPackage.*"
      suppress = true
    }
    perPackageOption {
      matchingRegex = "it.overriddenVisibility.*"
      documentedVisibilities(
        VisibilityModifier.PRIVATE,
      )
    }
    sourceLink {
      localDirectory = file("src/main")
      remoteUrl(
        "https://github.com/Kotlin/dokka/tree/master/integration-tests/gradle/projects/it-basic/src/main"
      )
    }
  }

  pluginsConfiguration.named<DokkaHtmlPluginParameters>("html") {
    customStyleSheets.from(
      "./customResources/logo-styles.css",
      "./customResources/custom-style-to-add.css",
    )
    customAssets.from(
      "./customResources/custom-resource.svg",
    )
  }

  dokkatooPublications.configureEach {
    suppressObviousFunctions = false
  }
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
  generator.dokkaSourceSets.configureEach {
    sourceSetScope = ":dokkaHtml"
  }
}
