import buildsrc.utils.args
import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.tasks.PathSensitivity.RELATIVE

plugins {
  buildsrc.conventions.base
  dev.adamko.`dokkatoo-html`
  com.github.`node-gradle`.node
  buildsrc.screenshotter.plugin
}

val docusaurusSiteDir = layout.projectDirectory.dir("site")
val docusaurusFiles = docusaurusSiteDir.asFileTree.matching {
  exclude(
    ".docusaurus",
    "node_modules",
  )
}

dependencies {
  dokkatoo(projects.modules.dokkatooPlugin)
}

dokkatoo {
  moduleName.set("Dokkatoo Gradle Plugin")

  dokkatooPublications.configureEach {
    includes.from("Module.md")
  }

  dokkatooPublicationDirectory.set(layout.buildDirectory.dir("dokka/html"))

  pluginsConfiguration {
    html {
      customAssets.from(
        docusaurusSiteDir.files(
          "static/img/logo-icon.svg",
          "static/img/homepage.svg",
        )
      )
      homepageLink = "https://github.com/adamko-dev/dokkatoo/"
      footerMessage = "Copyright © 2023"
    }
  }
}

node {
  nodeProjectDir = docusaurusSiteDir
}

val docusaurusPrepareKdoc by tasks.registering(Sync::class) {
  from(tasks.dokkatooGeneratePublicationHtml)
  into(docusaurusSiteDir.dir("static/kdoc"))
}

val docusaurusRun by tasks.registering(NpmTask::class) {
  group = "documentation"
  args("run start")

  inputs.files(docusaurusFiles)
    .withPropertyName("docusaurusFiles")
    .withPathSensitivity(RELATIVE)
    .normalizeLineEndings()

  dependsOn(tasks.npmInstall)
  dependsOn(docusaurusPrepareKdoc)
}

val docusaurusBuild by tasks.registering(NpmTask::class) {
  group = "documentation"
  args("run docusaurus build")

  inputs.files(docusaurusFiles)
    .withPropertyName("docusaurusFiles")
    .withPathSensitivity(RELATIVE)
    .normalizeLineEndings()

  dependsOn(tasks.npmInstall)
  dependsOn(docusaurusPrepareKdoc)

  val outputDir = layout.projectDirectory.dir("site/build")
  outputs.dir(outputDir).withPropertyName("outputDir")

  outputs.cacheIf { true }
}


val npmOutdated by tasks.registering(NpmTask::class) {
  group = "npm"
  args("outdated")
}

val npmUpdate by tasks.registering(NpmTask::class) {
  group = "npm"
  args("update")
}

tasks.withType<NpmTask>().configureEach {
  environment.put("NODE_OPTIONS", "--trace-deprecation")
}

tasks.clean {
  delete(docusaurusSiteDir.dir(".docusaurus"))
  delete(docusaurusSiteDir.dir("node_modules"))
}

tasks.screenshotter {
  website("GW2ChatLinks", "https://gw2toolbelt.github.io/GW2ChatLinks/latest/api/")
  website("KobanKat", "https://kobankat.shortway.io/")
  website("ModuleCheck", "https://rickbusarow.github.io/ModuleCheck/api/")
  website("androidx-ktx-extras", "https://edricchan03.github.io/androidx-ktx-extras/")
  website("apollo-graphql", "https://www.apollographql.com/docs/kotlin/kdoc/")
  website("dokkatoo", "https://adamko-dev.github.io/dokkatoo/")
  website("gradle", "https://docs.gradle.org/current/kotlin-dsl/")
  website("kotka-streams", "https://adamko-dev.github.io/kotka-streams/")
  website("ks3", "https://www.ks3.io/")
  website("oss-review-toolkit", "https://javadoc.io/doc/org.ossreviewtoolkit/cli/latest/")
  website("selfie", "https://kdoc.selfie.dev/")
  website("Prepared", "https://opensavvy.gitlab.io/prepared/api-docs/suite/index.html")
  website("Pedestal", "https://opensavvy.gitlab.io/pedestal/api-docs/")
}
