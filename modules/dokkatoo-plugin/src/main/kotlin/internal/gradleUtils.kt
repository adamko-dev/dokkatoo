package dev.adamko.dokkatoo.internal

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskProvider


/**
 * Mark this [Configuration] as one that will be consumed by other subprojects.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = true
 * ```
 */
internal fun Configuration.asProvider() {
  isCanBeResolved = false
  isCanBeConsumed = true
}

/**
 * Mark this [Configuration] as one that will consume artifacts from other subprojects (also known as 'resolving')
 *
 * ```
 * isCanBeResolved = true
 * isCanBeConsumed = false
 * ```
 * */
internal fun Configuration.asConsumer() {
  isCanBeResolved = true
  isCanBeConsumed = false
}


/** Invert a boolean [Provider] */
internal operator fun Provider<Boolean>.not(): Provider<Boolean> = map { !it }


/** Only matches components that come from subprojects */
internal object LocalProjectOnlyFilter : Spec<ComponentIdentifier> {
  override fun isSatisfiedBy(element: ComponentIdentifier?): Boolean =
    element is ProjectComponentIdentifier
}

/** Invert the result of a [Spec] predicate */
internal operator fun <T> Spec<T>.not() = Spec<T> { !this@not.isSatisfiedBy(it) }


internal fun Project.pathAsFilePath() = path
  .removePrefix(GradleProjectPath.SEPARATOR)
  .replace(GradleProjectPath.SEPARATOR, "/")


/**
 * Apply some configuration to a [Task] using
 * [configure][org.gradle.api.tasks.TaskContainer.configure],
 * and return the same [TaskProvider].
 */
internal fun <T : Task> TaskProvider<T>.configuring(
  block: Action<T>
): TaskProvider<T> = apply { configure(block) }


internal fun <T> NamedDomainObjectContainer<T>.maybeCreate(
  name: String,
  configure: T.() -> Unit,
): T = maybeCreate(name).apply(configure)


/**
 * Aggregate the incoming files from a [Configuration]
 * (with name [named]) into [collector].
 *
 * Configurations that cannot be
 * [resolved][org.gradle.api.artifacts.Configuration.isCanBeResolved]
 * will be ignored.
 *
 * @param[builtBy] An optional [TaskProvider], used to set [ConfigurableFileCollection.builtBy].
 * This should not typically be used, and is only necessary in rare cases where a Gradle Plugin is
 * misconfigured.
 */
internal fun ConfigurationContainer.collectIncomingFiles(
  named: String,
  collector: ConfigurableFileCollection,
  builtBy: TaskProvider<*>? = null,
  artifactViewConfiguration: ArtifactView.ViewConfiguration.() -> Unit = {
    // ignore failures: it's usually okay if fetching files is best-effort because
    // maybe Dokka doesn't need _all_ dependencies
    lenient(true)
  },
) {
  val conf = findByName(named)
  if (conf != null && conf.isCanBeResolved) {

    val incomingFiles =
      conf
        .incoming
        .artifactView(artifactViewConfiguration)
        .artifacts
        .artifactFiles

    collector.from(incomingFiles)

    if (builtBy != null) {
      collector.builtBy(builtBy)
    }
  }
}
