package dev.adamko.dokkatoo.utils


/**
 * Version of the Kotlin Gradle Plugin used in test projects.
 *
 * Must be compatible with the Gradle version used in tests, and the Kotlin metadata must be readable
 * by Dokka's Kotlin analysis.
 */
const val TESTED_KOTLIN_VERSION = "2.0.21"


/**
 * Gradle version used to run the integration test projects.
 *
 * The integration test projects use old versions of KGP, AGP, and Dokka that aren't compatible
 * with Gradle 9.
 */
const val INTEGRATION_TEST_GRADLE_VERSION = "8.14.5"
