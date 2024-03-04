plugins {
  buildsrc.conventions.base
  dev.adamko.`dokkatoo-html`
}

dependencies {
  dokkatoo(projects.modules.dokkatooPlugin)
}

dokkatoo {
  moduleName.set("Dokkatoo Gradle Plugin")

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
