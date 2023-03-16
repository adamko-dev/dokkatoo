package dev.adamko.dokkatoo.internal

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
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


/**
 * Only matches components that come from subprojects
 */
internal object LocalProjectOnlyFilter : Spec<ComponentIdentifier> {
  override fun isSatisfiedBy(element: ComponentIdentifier?): Boolean =
    element is ProjectComponentIdentifier
}


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
