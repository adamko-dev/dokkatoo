package dev.adamko.dokkatoo.utils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.DefaultGradleRunner

fun GradleRunner.withEnvironment(vararg map: Pair<String, String>): GradleRunner =
  withEnvironment(map.toMap())

inline fun GradleRunner.build(
  handleResult: BuildResult.() -> Unit
): Unit = build().let(handleResult)

inline fun GradleRunner.buildAndFail(
  handleResult: BuildResult.() -> Unit
): Unit = buildAndFail().let(handleResult)


fun GradleRunner.withJvmArguments(
  vararg jvmArguments: String
): GradleRunner = (this as DefaultGradleRunner).withJvmArguments(*jvmArguments)
