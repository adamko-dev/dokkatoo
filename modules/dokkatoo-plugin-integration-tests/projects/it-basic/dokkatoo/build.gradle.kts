import dev.adamko.dokkatoo.dokka.parameters.VisibilityModifier
import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters

plugins {
  kotlin("jvm") version "1.7.20"
  id("dev.adamko.dokkatoo") version "1.0.1-SNAPSHOT"
}

version = "1.7.20-SNAPSHOT"

dependencies {
  implementation(kotlin("stdlib"))
  testImplementation(kotlin("test-junit"))
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
      remoteUrl.set(
        uri(
          "https://github.com/Kotlin/dokka/tree/master/integration-tests/gradle/projects/it-basic/src/main"
        ).toURL()
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
    suppressObviousFunctions.set(true)
    suppressObviousFunctions.set(false)
  }
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask>().configureEach {
  dokkaSourceSets.configureEach {
    sourceSetScope.set(":dokkaHtml")
  }
}
