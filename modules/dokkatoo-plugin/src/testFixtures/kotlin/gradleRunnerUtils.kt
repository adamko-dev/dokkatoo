package dev.adamko.dokkatoo.utils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.DefaultGradleRunner


/** Edit environment variables in the Gradle Runner */
fun GradleRunner.withEnvironment(build: MutableMap<String, String?>.() -> Unit): GradleRunner {
  val env = environment ?: mutableMapOf()
  env.build()
  return withEnvironment(env)
}


inline fun GradleRunner.build(
  handleResult: BuildResult.() -> Unit
): Unit = build().let(handleResult)


inline fun GradleRunner.buildAndFail(
  handleResult: BuildResult.() -> Unit
): Unit = buildAndFail().let(handleResult)


fun GradleRunner.withJvmArguments(
  vararg jvmArguments: String
): GradleRunner = (this as DefaultGradleRunner).withJvmArguments(*jvmArguments)
