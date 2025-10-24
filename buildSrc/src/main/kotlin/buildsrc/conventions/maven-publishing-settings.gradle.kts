package buildsrc.conventions

import buildsrc.settings.MavenPublishingSettings

val mavenPublishing =
  extensions.create<MavenPublishingSettings>(MavenPublishingSettings.EXTENSION_NAME, project)
