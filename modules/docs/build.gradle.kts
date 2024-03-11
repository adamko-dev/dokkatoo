import buildsrc.utils.args
import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.tasks.PathSensitivity.RELATIVE

plugins {
  buildsrc.conventions.base
  dev.adamko.`dokkatoo-html`
  com.github.`node-gradle`.node
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

tasks.clean {
  delete(docusaurusSiteDir.dir(".docusaurus"))
  delete(docusaurusSiteDir.dir("node_modules"))
}
