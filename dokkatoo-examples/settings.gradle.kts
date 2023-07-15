import dokkatoo.utils.gitHubRelease

rootProject.name = "dokkatoo-examples"

pluginManagement {
  includeBuild("../build-tools/build-plugins")
  includeBuild("../build-tools/settings-plugins")
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("dokkatoo.conventions.settings-baseww")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

  repositories {
    mavenCentral()
    gradlePluginPortal()
    gitHubRelease()
  }

  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}

includeBuild("../dokkatoo-components")

includeBuild("custom-format-example-dokka")
includeBuild("custom-format-example-dokkatoo")
includeBuild("gradle-example-dokka")
includeBuild("gradle-example-dokkatoo")
includeBuild("kotlin-as-java-example-dokka")
includeBuild("kotlin-as-java-example-dokkatoo")
includeBuild("library-publishing-example-dokka")
includeBuild("library-publishing-example-dokkatoo")
//includeBuild("multimodule-example-dokka")
includeBuild("multimodule-example-dokkatoo")
//includeBuild("multiplatform-example-dokka")
//includeBuild("multiplatform-example-dokkatoo")
//includeBuild("versioning-multimodule-example-dokka")
includeBuild("versioning-multimodule-example-dokkatoo")

//listOf(
//  "custom-format-example",
//  "gradle-example",
//  "kotlin-as-java-example",
//  "library-publishing-example",
//  "multimodule-example",
//  "multiplatform-example",
//  "versioning-multimodule-example",
//).forEach { exampleDir ->
//  listOf(
//    "dokka",
//    "dokkatoo",
//  ).forEach { dokka ->
//    includeBuild("./$exampleDir/$dokka/") {
//      name = "$exampleDir-$dokka"
//    }
//  }
//}
