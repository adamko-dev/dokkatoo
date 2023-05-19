package buildsrc.conventions

plugins {
  `maven-publish`
}

val githubPublishDir: Provider<File> =
  providers.environmentVariable("GITHUB_PUBLISH_DIR").map { file(it) }

publishing {
  repositories {
    maven(githubPublishDir) {
      name = "GitHubPublish"
    }
  }
}
