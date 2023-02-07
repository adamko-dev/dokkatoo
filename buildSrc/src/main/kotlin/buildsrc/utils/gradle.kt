package buildsrc.conventions.utils

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RelativePath

/**
 * Mark this [Configuration] as one that will be consumed by other subprojects.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = true
 * ```
 */
fun Configuration.asProvider() {
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
fun Configuration.asConsumer() {
  isCanBeResolved = true
  isCanBeConsumed = false
}


/** Drop the first [count] directories from the path */
fun RelativePath.dropDirectories(count: Int): RelativePath =
  RelativePath(true, *segments.drop(count).toTypedArray())


/** Drop the first directory from the path */
fun RelativePath.dropDirectory(): RelativePath =
  dropDirectories(1)


/** Drop the first directory from the path */
fun RelativePath.dropDirectoriesWhile(
  segmentPrediate: (segment: String) -> Boolean
): RelativePath =
  RelativePath(
    true,
    *segments.dropWhile(segmentPrediate).toTypedArray(),
  )
