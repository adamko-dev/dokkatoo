plugins {
  buildsrc.conventions.base
  dev.adamko.`dokkatoo-html`
}

dependencies {
  dokkatoo(projects.modules.dokkatooPlugin)
}

dokkatoo {
  moduleName.set("Dokkatoo Gradle Plugin")

  dokkatooPublications.configureEach {
    includes.from("Module.md")
  }

  // generate into a nested /kdoc/ directory for GitHub Pages
  dokkatooPublicationDirectory.set(layout.buildDirectory.dir("dokka/html/kdoc"))

  pluginsConfiguration {
    html {
      customAssets.from(
        "./images/logo-icon.svg",
        "./images/homepage.svg",
      )
      homepageLink = "https://github.com/adamko-dev/dokkatoo/"
    }
  }
}
