package buildsrc.utils

import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs

fun NpmTask.args(values: String) {
  args.set(parseSpaceSeparatedArgs(values))
}

fun NpmTask.args(values: Provider<String>) {
  args.addAll(values.map(::parseSpaceSeparatedArgs))
}
