import dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier
import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters

plugins {
  kotlin("jvm") version "1.9.10"
  id("dev.adamko.dokkatoo") version "2.1.0"
}

version = "1.9.10-SNAPSHOT"

dependencies {
  testImplementation(kotlin("test-junit"))
}

kotlin {
  jvmToolchain(8)
}

dokkatoo {
  moduleName.set("Basic Project")
  dokkatooSourceSets.configureEach {
    documentedVisibilities(
      VisibilityModifier.PUBLIC,
      VisibilityModifier.PROTECTED,
    )
    suppressedFiles.from(file("src/main/kotlin/it/suppressedByPath"))
    perPackageOption {
      matchingRegex.set("it.suppressedByPackage.*")
      suppress.set(true)
    }
    perPackageOption {
      matchingRegex.set("it.overriddenVisibility.*")
      documentedVisibilities(
        VisibilityModifier.PRIVATE,
      )
    }
    sourceLink {
      localDirectory.set(file("src/main"))
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
    suppressObviousFunctions.set(false)
  }
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
  generator.dokkaSourceSets.configureEach {
    sourceSetScope.set(":dokkaHtml")
  }
}
