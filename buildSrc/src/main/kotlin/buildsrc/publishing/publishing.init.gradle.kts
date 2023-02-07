gradle.allprojects {
  val mavenProjectRepoDir =
    providers.environmentVariable("LOCAL_PROJECT_PUBLISH_DIR").map { file(it) }

  plugins.withType<MavenPublishPlugin>().configureEach {
    extensions.configure<PublishingExtension> {
      repositories {
        if (mavenProjectRepoDir.isPresent) {
          maven(mavenProjectRepoDir) {
            name = "MavenProjectRepo"
          }
        }
      }
    }
  }
}
