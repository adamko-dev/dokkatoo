import org.jetbrains.dokka.DokkaConfiguration

plugins {
  kotlin("jvm") version "1.7.20"
  id("dev.adamko.dokkatoo") version "0.0.4-SNAPSHOT"
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
      DokkaConfiguration.Visibility.PUBLIC,
      DokkaConfiguration.Visibility.PROTECTED,
    )
    suppressedFiles.from(file("src/main/kotlin/it/suppressedByPath"))
    perPackageOption {
      matchingRegex.set("it.suppressedByPackage.*")
      suppress.set(true)
    }
    perPackageOption {
      matchingRegex.set("it.overriddenVisibility.*")
      documentedVisibilities.set(
        setOf(DokkaConfiguration.Visibility.PRIVATE)
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
  dokkatooPublications.configureEach {
    suppressObviousFunctions.set(true)
    pluginsConfiguration.create("org.jetbrains.dokka.base.DokkaBase") {
      serializationFormat.set(DokkaConfiguration.SerializationFormat.JSON)
      values.set(
        """
          { 
            "customStyleSheets": [
              "${file("./customResources/logo-styles.css").invariantSeparatorsPath}", 
              "${file("./customResources/custom-style-to-add.css").invariantSeparatorsPath}"
            ], 
            "customAssets": [
              "${file("./customResources/custom-resource.svg").invariantSeparatorsPath}"
            ] 
          }
        """.trimIndent()
      )
    }
    suppressObviousFunctions.set(false)
  }
}

tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooPrepareParametersTask>().configureEach {
  dokkaSourceSets.configureEach {
    sourceSetScope.set(":dokkaHtml")
  }
}
